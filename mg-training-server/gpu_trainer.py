from __future__ import annotations
import argparse, json, os, sys, time
from pathlib import Path


def build_config(args):
    return {
        'base_model': args.base_model,
        'dataset': args.dataset,
        'output_dir': args.output_dir,
        'method': args.method,
        'epochs': args.epochs,
        'learning_rate': args.learning_rate,
        'batch_size': args.batch_size,
        'gradient_accumulation_steps': args.gradient_accumulation_steps,
        'max_seq_length': args.max_seq_length,
        'lora_r': args.lora_r,
        'lora_alpha': args.lora_alpha,
        'lora_dropout': args.lora_dropout,
        'resume_from_checkpoint': args.resume_from_checkpoint,
    }


def require_gpu():
    if os.getenv('MG_TRAIN_GPU_AVAILABLE','0') != '1':
        raise RuntimeError('gpu_unavailable')


def load_jsonl(path: str):
    rows=[]
    with open(path,'r',encoding='utf-8') as f:
        for line in f:
            line=line.strip()
            if not line: continue
            obj=json.loads(line)
            if 'text' not in obj: raise ValueError('dataset_row_missing_text')
            rows.append({'text':obj['text']})
    if not rows: raise ValueError('empty_dataset')
    return rows


def train(args):
    require_gpu()
    try:
        import torch
        from datasets import Dataset
        from transformers import AutoTokenizer, AutoModelForCausalLM, TrainingArguments, Trainer, DataCollatorForLanguageModeling, BitsAndBytesConfig
        from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
    except Exception as e:
        raise RuntimeError('training_dependencies_unavailable:'+str(e))

    rows=load_jsonl(args.dataset)
    ds=Dataset.from_list(rows)
    tokenizer=AutoTokenizer.from_pretrained(args.base_model, trust_remote_code=True)
    if tokenizer.pad_token is None: tokenizer.pad_token=tokenizer.eos_token

    def tokenize(batch):
        return tokenizer(batch['text'], truncation=True, max_length=args.max_seq_length, padding=False)
    tokenized=ds.map(tokenize, batched=True, remove_columns=['text'])

    quant=None
    kwargs={'trust_remote_code':True,'device_map':'auto'}
    if args.method=='qlora_sft':
        quant=BitsAndBytesConfig(load_in_4bit=True,bnb_4bit_quant_type='nf4',bnb_4bit_compute_dtype=torch.bfloat16,bnb_4bit_use_double_quant=True)
        kwargs['quantization_config']=quant
    else:
        kwargs['torch_dtype']=torch.bfloat16 if torch.cuda.is_bf16_supported() else torch.float16

    model=AutoModelForCausalLM.from_pretrained(args.base_model, **kwargs)
    if args.method=='qlora_sft': model=prepare_model_for_kbit_training(model)
    lora=LoraConfig(r=args.lora_r,lora_alpha=args.lora_alpha,lora_dropout=args.lora_dropout,bias='none',task_type='CAUSAL_LM',target_modules='all-linear')
    model=get_peft_model(model,lora)

    out=Path(args.output_dir); out.mkdir(parents=True,exist_ok=True)
    targs=TrainingArguments(
        output_dir=str(out), num_train_epochs=args.epochs, learning_rate=args.learning_rate,
        per_device_train_batch_size=args.batch_size, gradient_accumulation_steps=args.gradient_accumulation_steps,
        logging_steps=10, save_steps=args.save_steps, save_total_limit=3,
        bf16=torch.cuda.is_bf16_supported(), fp16=not torch.cuda.is_bf16_supported(),
        report_to='none', remove_unused_columns=False
    )
    collator=DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm=False)
    trainer=Trainer(model=model,args=targs,train_dataset=tokenized,data_collator=collator)
    started=time.time(); result=trainer.train(resume_from_checkpoint=args.resume_from_checkpoint or None)
    trainer.save_model(str(out/'adapter')); tokenizer.save_pretrained(str(out/'adapter'))
    metrics=dict(result.metrics); metrics['wall_time_s']=time.time()-started; metrics['method']=args.method; metrics['base_model']=args.base_model
    with open(out/'training_metrics.json','w',encoding='utf-8') as f: json.dump(metrics,f,ensure_ascii=False,indent=2)
    with open(out/'training_config.json','w',encoding='utf-8') as f: json.dump(build_config(args),f,ensure_ascii=False,indent=2)
    return metrics


def parser():
    p=argparse.ArgumentParser()
    p.add_argument('--base-model',required=True)
    p.add_argument('--dataset',required=True)
    p.add_argument('--output-dir',required=True)
    p.add_argument('--method',choices=['lora_sft','qlora_sft'],default='lora_sft')
    p.add_argument('--epochs',type=float,default=1.0)
    p.add_argument('--learning-rate',type=float,default=2e-4)
    p.add_argument('--batch-size',type=int,default=1)
    p.add_argument('--gradient-accumulation-steps',type=int,default=16)
    p.add_argument('--max-seq-length',type=int,default=2048)
    p.add_argument('--lora-r',type=int,default=16)
    p.add_argument('--lora-alpha',type=int,default=32)
    p.add_argument('--lora-dropout',type=float,default=0.05)
    p.add_argument('--save-steps',type=int,default=100)
    p.add_argument('--resume-from-checkpoint',default='')
    p.add_argument('--dry-run',action='store_true')
    return p

if __name__=='__main__':
    args=parser().parse_args()
    if args.dry_run:
        print(json.dumps(build_config(args),ensure_ascii=False,indent=2)); sys.exit(0)
    try:
        print(json.dumps(train(args),ensure_ascii=False,indent=2))
    except Exception as e:
        print(str(e),file=sys.stderr); sys.exit(2)
