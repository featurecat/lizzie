VERSION = 210826a
ZIP_NAME = tmp_Lizzie-patched-0.7.4_$(VERSION).zip
FILES = Lizzie.vbs README.html README_ja.html config.txt jar jre katago

# [2021-07-10]
# The official URL for the 15 block network is unusable temporarily.
#   https://d3dndmfyhecmj0.cloudfront.net/g170/neuralnets/index.html
#   https://github.com/lightvector/KataGo/issues/522
# So...
# (1) Run "make kata" and "make zip" in github actions.
# (2) Download created tmp_Lizzie-patched-*.zip to a local PC.
# (3) Put a local copy of the best 15b net on KATA_MODEL_PATH.
# (4) Run "make add_model_to_zip" locally.

KATA_DIR = katago
KATA_URL1 = https://github.com/lightvector/KataGo/releases/download/v1.9.1/katago-v1.9.1-eigen-windows-x64.zip
KATA_URL2 = https://github.com/lightvector/KataGo/releases/download/v1.9.1/katago-v1.9.1-eigenavx2-windows-x64.zip
KATA_URL3 = https://github.com/lightvector/KataGo/releases/download/v1.9.1/katago-v1.9.1-opencl-windows-x64.zip
# KATA_MODEL_URL = XXXXXXXXXXXXX

KATA_MODEL_PATH = $(KATA_DIR)/default_model.bin.gz

zip:
	zip -r $(ZIP_NAME) $(FILES)

kata:
	\wget -O tmp_kata1.zip $(KATA_URL1)
	\wget -O tmp_kata2.zip $(KATA_URL2)
	\wget -O tmp_kata3.zip $(KATA_URL3)
	cd $(KATA_DIR); unzip -o ../tmp_kata1.zip; mv katago.exe katago-eigen.exe
	cd $(KATA_DIR); unzip -o ../tmp_kata2.zip; mv katago.exe katago-eigenavx2.exe
	cd $(KATA_DIR); unzip -o ../tmp_kata3.zip; mv katago.exe katago-opencl.exe
#	cd $(KATA_DIR); \wget -O default_model.bin.gz $(KATA_MODEL_URL)

add_model_to_zip:
	zip $(ZIP_NAME) $(KATA_MODEL_PATH)
