VERSION = 250405a
ZIP_NAME = tmp_Lizzie-patched-0.7.4_$(VERSION).zip
FILES = Lizzie.vbs README.html README_ja.html config.txt jar jre katago

KATA_DIR = katago
# KATA_URL1 = https://github.com/lightvector/KataGo/releases/download/v1.15.1/katago-v1.15.1-eigen-windows-x64.zip
KATA_URL2 = https://github.com/lightvector/KataGo/releases/download/v1.15.1/katago-v1.15.1-eigenavx2-windows-x64.zip
KATA_URL3 = https://github.com/lightvector/KataGo/releases/download/v1.15.1/katago-v1.15.1-opencl-windows-x64.zip
KATA_MODEL_URL = https://media.katagotraining.org/uploaded/networks/models/kata1/kata1-b18c384nbt-s9996604416-d4316597426.bin.gz
KATA_HUMANMODEL_URL = https://github.com/lightvector/KataGo/releases/download/v1.15.0/b18c384nbt-humanv0.bin.gz

zip:
	zip -r $(ZIP_NAME) $(FILES)

kata:
	mkdir -p $(KATA_DIR)
#	\wget -O tmp_kata1.zip $(KATA_URL1)
	\wget -O tmp_kata2.zip $(KATA_URL2)
	\wget -O tmp_kata3.zip $(KATA_URL3)
#	cd $(KATA_DIR); unzip -o ../tmp_kata1.zip; mv katago.exe katago-eigen.exe
	cd $(KATA_DIR); unzip -o ../tmp_kata2.zip; mv katago.exe katago-eigenavx2.exe
	cd $(KATA_DIR); unzip -o ../tmp_kata3.zip; mv katago.exe katago-opencl.exe
	cd $(KATA_DIR); \wget -O default_model.bin.gz $(KATA_MODEL_URL)
	cd $(KATA_DIR); \wget -O human_model.bin.gz $(KATA_HUMANMODEL_URL)
