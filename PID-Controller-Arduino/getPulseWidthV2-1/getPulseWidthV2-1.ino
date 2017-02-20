
const int PIN_MTR1  = 5;    //LowPim
const int PIN_MTR2  = 6;    //HighPin
const int PIN_INTR  = 2;

const byte PWM_MAX = 220;
const byte PWM_MIN = 10; 
const int TIME_WIDTH  = 50;


volatile unsigned long timeStart = 0;    //パルス数えはじめの時刻
volatile unsigned long cnt = 0;
boolean flgCntStart = true;


//Negエッジ割り込みで呼ばれてパルスを数える
//引数：なし
//返値：なし
void getPulseNum(){
  delayMicroseconds(10);
  if(digitalRead(PIN_INTR)==HIGH){
    cnt++;
  }
}


//モータ出力の上限・下限を超えていないか確認
//引数：通信で指示された値
//返値：閾値超えてたら既定の上限・下限の値、超えていなければ引数そのまま,但し0は0で通す
byte chkLim(byte p_data){
  
  if(p_data > PWM_MAX){
    return PWM_MAX;
  }
  else if(p_data < PWM_MIN && p_data != 0){
    return PWM_MIN;
  }
  else {
    return p_data;
  }
}


void setup(){
  
  pinMode(PIN_INTR, INPUT);
  pinMode(PIN_MTR1, OUTPUT);
  pinMode(PIN_MTR2, OUTPUT);
  
  attachInterrupt(0, getPulseNum, RISING);
  
  Serial.begin(115200);
}


  byte dataChked = 0;
  byte tmp = 1;
 
void loop(){

  static float pulseWidth;

      
  unsigned long width = millis() - timeStart;
  
  if(flgCntStart==true){
    
    timeStart = millis();
    cnt = 0;
    flgCntStart = false;

  }
  else if(width > TIME_WIDTH){
    
    pulseWidth = ((float)width * 1000/(float)cnt); 
    
    const byte aveSample = 5;
    static float sens[aveSample];
    int sum = 0;
    
    for(int i=0; i<aveSample; i++){
      sens[i] = sens[i+1];
    }
    sens[aveSample] = pulseWidth; 
    for(int i=0; i<aveSample; i++){
      sum += sens[i];
    }
    
    float pulseWidthFilted = sum /aveSample -70;
    
    //Serial.println(pulseWidthFilted);             //for debug
    Serial.print((char)pulseWidthFilted);               //for pid control
    flgCntStart = true;
    delay(5);
  }

    

  if (Serial.peek() != -1) {
     tmp = Serial.read();
  }
  
  dataChked = chkLim(tmp);
  int mtrVal = 0.6*dataChked + 75;
  analogWrite(PIN_MTR1, 20);
  analogWrite(PIN_MTR2, dataChked);
  //Serial.print((char)dataChked);
  //delay(5); 

  /*
  //一次系シュミレート
  
    static int y[2];
    const char Ts = 100;
    const char Tf = 1;
    y[0] = y[1];
    y[1] = ((Ts*tmp)+(Tf*y[0]) )/(Ts+Tf);
        
    Serial.print((char)y[1]);             //for debug
    delay(50);
  */
}

