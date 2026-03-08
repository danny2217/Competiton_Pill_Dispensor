#include <Wire.h>
#include <SoftwareSerial.h>

// 블루투스와 통신할 가짜 귀(D2), 입(D3) 만들기
SoftwareSerial BTSerial(2, 3); 

const int MPU_ADDR = 0x68;
int16_t AcX, AcY, AcZ;

void setup() {
  Serial.begin(9600);     // PC와 통신 속도
  BTSerial.begin(9600);   // 블루투스와 통신 속도 (HC-06/05 기본값)
  
  Wire.begin();
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x6B);
  Wire.write(0);
  Wire.endTransmission(true);
  
  Serial.println("블루투스 & 센서 준비 완료!");
}

void loop() {
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(0x3B);
  Wire.endTransmission(false);
  Wire.requestFrom(MPU_ADDR, 6, true);

  AcX = Wire.read() << 8 | Wire.read(); 
  AcY = Wire.read() << 8 | Wire.read(); 
  AcZ = Wire.read() << 8 | Wire.read(); 

  // // --- 자세 판별 로직 ---
  // // (주의: 기준값 12000이나 축 이름은 직접 테스트하면서 맞게 고치세요!)
  // String postureCode = "0"; // 기본값 (알 수 없음)

  // if (abs(AcY) > 12000) {
  //   postureCode = "1"; // 서있음
  // } 
  // else if (abs(AcZ) > 12000) {
  //   postureCode = "2"; // 앉아있음
  // }
  // else if (abs(AcX) > 12000) {
  //   postureCode = "3"; // 누워있음
  // }

  // // 1. PC 모니터로 확인하기 (디버깅용)
  // Serial.print("현재 자세 코드: ");
  // Serial.println(postureCode);

  // // 2. 스마트폰으로 진짜 발사하기! (앱으로 날아가는 데이터)
  // BTSerial.println(postureCode); 

// 1. PC 시리얼 모니터에 출력 (확인용)
  Serial.print("X: "); Serial.print(AcX);
  Serial.print(" | Y: "); Serial.print(AcY);
  Serial.print(" | Z: "); Serial.println(AcZ);

  // 2. 스마트폰(블루투스 터미널)으로 예쁘게 쏴주기!
  BTSerial.print("X: "); BTSerial.print(AcX);
  BTSerial.print("  Y: "); BTSerial.print(AcY);
  BTSerial.print("  Z: "); BTSerial.println(AcZ);

  delay(500); // 0.5초마다 데이터 발사
}