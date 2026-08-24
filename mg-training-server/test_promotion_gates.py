from training_pipeline import TrainingRegistry, TrainingState
r=TrainingRegistry(); c=r.create('Qwen/Qwen3.5-4B','verified-dataset'); r.dataset_gate(c.id,500,0,0.93); assert c.state==TrainingState.DATA_READY; r.mark_training(c.id); r.register_checkpoint(c.id,'checkpoints/mg-ai-lora-001'); r.evaluation_gate(c.id,0.91,0.01,True); assert c.state==TrainingState.PROMOTABLE
try:r.promote(c.id,False); raise AssertionError('approval gate failed')
except ValueError:pass
r.promote(c.id,True); assert c.state==TrainingState.PROMOTED
bad=r.create('Qwen/Qwen3.5-4B','unsafe-dataset'); r.dataset_gate(bad.id,1000,1,0.99); assert bad.state==TrainingState.REJECTED
print('promotion gates OK')
