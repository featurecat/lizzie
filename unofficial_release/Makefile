VERSION = 231231a
ZIP_NAME = tmp_Lizzie-patched-0.7.4_$(VERSION).zip
FILES = Lizzie.vbs README.html README_ja.html config.txt jar jre katago

KATA_DIR = katago
KATA_URL1 = https://github.com/lightvector/KataGo/releases/download/v1.14.0/katago-v1.14.0-eigen-windows-x64.zip
KATA_URL2 = https://github.com/lightvector/KataGo/releases/download/v1.14.0/katago-v1.14.0-eigenavx2-windows-x64.zip
KATA_URL3 = https://github.com/lightvector/KataGo/releases/download/v1.14.0/katago-v1.14.0-opencl-windows-x64.zip
KATA_MODEL_URL = https://media.katagotraining.org/uploaded/networks/models/kata1/kata1-b15c192-s1672170752-d466197061.txt.gz

zip:
	zip -r $(ZIP_NAME) $(FILES)

kata:
	\wget -O tmp_kata1.zip $(KATA_URL1)
	\wget -O tmp_kata2.zip $(KATA_URL2)
	\wget -O tmp_kata3.zip $(KATA_URL3)
	cd $(KATA_DIR); unzip -o ../tmp_kata1.zip; mv katago.exe katago-eigen.exe
	cd $(KATA_DIR); unzip -o ../tmp_kata2.zip; mv katago.exe katago-eigenavx2.exe
	cd $(KATA_DIR); unzip -o ../tmp_kata3.zip; mv katago.exe katago-opencl.exe
	cd $(KATA_DIR); \wget -O default_model.txt.gz $(KATA_MODEL_URL)
