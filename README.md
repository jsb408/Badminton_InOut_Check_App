# 배드민턴 IN/OUT 판독기

배드민턴 경기 시 IN/OUT을 판독해주는 앱

- 진행기간 : 2021. 01. 18 ~ 2021. 02. 21
- 사용기술 : Android Studio, Kotlin, Tensorflow Lite

<p align="center"><img src="https://user-images.githubusercontent.com/55052074/115536005-420f5680-a2d4-11eb-899c-00e1743a2d15.jpg" width="30%"></p>

## 서비스 소개

- 배드민턴 IN/OUT 판독기는 배드민턴 경기 시 인공지능을 활용해 셔틀콕이 라인을 넘었는지 여부를 판독해주는 안드로이드 앱입니다.
- 갤러리에서 저장된 경기 영상을 가져와 분석 결과를 확인할 수 있습니다.
- 미디어 플레이어를 제공하여 원하는 부분의 분석을 확인할 수 있습니다.

## 상세 기능 소개

### 1. 동영상 선택

<p align="center"><img src="https://user-images.githubusercontent.com/55052074/115536081-59e6da80-a2d4-11eb-9b8e-574e82657cc5.jpg" width="30%"> <img src="https://user-images.githubusercontent.com/55052074/115536093-5e12f800-a2d4-11eb-8ab0-2a390f1d8b24.jpg" width="30%"></p>
<p align="center"><img src="https://user-images.githubusercontent.com/55052074/115536100-5f442500-a2d4-11eb-9f5f-bce7672f9655.jpg" width="60%">
  
- 동영상 판독 버튼을 터치하면 갤러리에서 동영상을 가져올 수 있습니다.
- 원하는 동영상을 선택하면 동영상 분석이 시작됩니다.
- 분석이 완료되면 결과 화면으로 넘어갑니다.

### 2. 분석 결과

<p align="center"><img src="https://user-images.githubusercontent.com/55052074/115536285-97e3fe80-a2d4-11eb-9a1d-c7bc63a55202.jpg" width="45%"> <img src="https://user-images.githubusercontent.com/55052074/115536268-9286b400-a2d4-11eb-8eab-3bb860b7d5a2.jpg" width="45%"></p>

- 동영상에서 셔틀콕의 위치를 분석해 라인을 넘었는지 여부를 판단해 IN/OUT 형태로 표시합니다.

### 2-1. 미디어 플레이어

<p align="center"><img src="https://user-images.githubusercontent.com/55052074/115536491-cf52ab00-a2d4-11eb-9320-0f61ee6b9189.jpg" width="60%"></p>

- 미디어 플레이어를 제공하여 분석 결과를 보고싶은 부분으로 바로 이동이 가능합니다.

## 보완 사항

- Movement Detection으로 변경
<br/>현재는 Object Detection으로 셔틀콕의 위치를 판별하여 IN과 OUT을 판독하지만 Movement Detection으로 셔틀콕이 땅에 닿는 순간의 움직임을 판별한다면 더 정확한 결과를 얻을 수 있을 것입니다.
- 라인 사용자화
<br/>현재는 라인이 테스트 영상에 맞게 고정된 값으로 판독이 되고 있습니다. 이를 사용자가 설정할 수 있도록 변경해 범용성을 확보할 수 있게 하고 싶습니다.
