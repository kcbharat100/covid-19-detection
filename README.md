# covid-19-detection-using-x-rays

The detection of covid-19 is based on X-rays using deep learning model.

Covid-19 X-rays have been taken from the paper:
"COVID-19 Image Data Collection: Prospective Predictions Are the Future"
Joseph Paul Cohen, Paul Morrison, Lan Dao, Karsten Roth, Tim Q Duong, Marzyeh Ghassemi
https://github.com/ieee8023/covid-chestxray-dataset

Normal X-rays have been taken from:
https://www.kaggle.com/paultimothymooney/chest-xray-pneumonia

No_Findings and Pneumonia X-rays have been taken from the paper:
"Automated detection of COVID-19 cases using deep neural networks with X-ray images"
TulinOzturk, MuhammedTalo, Eylul AzraYildirim, Ulas BaranBaloglu, OzalYildirim, U.Rajendra Acharya
https://github.com/muhammedtalo/COVID-19

Directory dataset_binary contains 500 "normal" and 206 "covid" xrays.
Directory dataset_multi_class contains 500 "no_findings", 500 "pneumonia" and 206 "covid" xrays.

Binary classification has been done and the current accuracy is 98.81% on test data.
