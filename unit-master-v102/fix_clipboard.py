from pathlib import Path
p=Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
s=p.read_text(encoding='utf-8')
s=s.replace('ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);','android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);')
p.write_text(s,encoding='utf-8')
print('Fixed ClipboardManager ambiguity')
