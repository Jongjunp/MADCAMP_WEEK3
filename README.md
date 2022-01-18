# 몰입캠프 1주차 과제

## 목차

+ [프로젝트 개요](#프로젝트-개요)
    + [목표](#목표)
    + [목적](#목적)
    + [사용 언어, 툴](#사용-언어-툴)
    + [결과물](#결과물)


+ [탭별 주요코드 설명](#탭별-주요코드-설명)
    + [Gallery](#Gallery)
    + [PhoneBook](#PhoneBook)
    + [StopWatch](#StopWatch)
    + [Object Recognition](#Object-Recognition)
    + [MetaMong](#MetaMong)

+ [구현 결과](#구현-결과)
    + [Gallery](#1-갤러리)
    + [PhoneBook](#2-연락처)
    + [StopWatch](#3-스톱워치)
    + [Object Recognition](#4-사물인식)
    + [MetaMong](#5-메타몽)

## 프로젝트 개요

### 목표
  + 탭 구조를 활용한 안드로이드 앱 제작
  
### 목적
  + 서로 함께 공통의 과제를 함으로써 개발에 빠르게 익숙해지기
  
### 사용 언어, 툴
  + JAVA
  + Kotlin
  + android studio
  + tensorflow lite
  + ARCore
  
### 결과물
  + 세개의 탭이 존재하는 안드로이드 앱
  + 탭1 : 나만의 이미지 갤러리 구축
  + 탭2 : 나의 연락처 구축, 휴대폰의 연락처 데이터 활용
  + 탭3 : 자유 주제(스톱워치 구현)
  + 탭4 : 텐서플로우 Lite를 활용한 사물인식 탭
    + _해당 탭은 탭1을 추가 기능 구현중 코드가 복잡해져서 분리함_
  + 탭5 : ARCore를 활용한 얼굴인식 및 가면 스티커 탭
    + _해당 탭은 탭1을 추가 기능 구현중 코드가 복잡해져서 분리함_

---------------------------

## 탭별 주요코드 설명

### Gallery

+ 앨범
  + 갤러리의 모든 리소스에 대한 uri(Uniform Resource Identifier : 일종으 자원 식별자)를 구해 앨범 그리드뷰 리스트에 추가

  ```java
  private fun getAllShownImagesPath() {
        //contentResolver의 데이터타입과 가져오는 주소(갤러리) 정의
        val uriExternal: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        
        //이미지 인덱스 번호 및 아이디
        var columnIndexID: Int
        var imageId: Long
        
        //contentResolver 정의 및 생성
        val cursor = contentResolver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"
        )
        if (cursor != null) {
            // 매 루프마다 contentResolver에 쿼리를 하여 모든 이미지 리소스를 순회
            while (cursor.moveToNext()) {
            
                // 사진 경로 Uri 가져오기
                columnIndexID = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                imageId = cursor.getLong(columnIndexID)
                val uriImage = Uri.withAppendedPath(uriExternal, "" + imageId)
                
                // 그리드뷰 어댑터에 uri를 추가하여 앨범에 해당 리소스 표시
                pictureAdapter.addItem(PictureItem(uriImage))
            }
            cursor.close()
        }
    }
  ```

+ 카메라
  + Preview

  ```kotlin
  private fun openCamera() {
  
      //카메라 프로바이더 객체
      val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
      
      //카메라 프로바이더 객체에 리스너를 등록하여 프리뷰에 객체의 화면을 출력
      cameraProviderFuture.addListener({
          val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

          val preview = Preview.Builder()
              .build()
              .also {
                  it.setSurfaceProvider(previewView.surfaceProvider)
              }
          //카메라 캡쳐 UseCase 등록
          imageCapture = ImageCapture.Builder()
              .build()

          //후면 카메라 선택
          val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

          //화며 바인딩
          try {

              cameraProvider.unbindAll()
              cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
              Log.d("TAG", "바인딩 성공")
  
          } catch (e: Exception) {
              Log.d("TAG", "바인딩 실패 $e")
          }
      }, ContextCompat.getMainExecutor(this))
  }
  ```

  + Camera Capture 및 갤러리 저장

  ```kotlin
  private fun CameraCapture() {
      이미지 캡쳐
      imageCapture = imageCapture ?: return
      val fileName:String = "CS496_" + System.currentTimeMillis().toString() + ".png"
      
      //사진 파일 생성, 일단 write 권한이 있는 cache 디렉토리에 저장
      val photoFile = File(cacheDir,fileName)
      
      Log.d(TAG,"photoFile : ${photoFile.toString()}")
      val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
      imageCapture?.takePicture(
          outputOption,
          ContextCompat.getMainExecutor(this),
          object : ImageCapture.OnImageSavedCallback {
              override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                  savedUri = Uri.fromFile(photoFile)

                  //contentResolver를 통해 savedUri에 해당하는 이미지를 byte 포맷으로 변환 후 FileOutputStream을 통해 캐시 디렉토리에 저장한 카메라 캡쳐 이미지를 외부저장소(갤러리)에 복사하는 함수 
                  saveFile()
                  
                  //사진 추가 복사 후 새로 찍은 사진을 gridview에 추가후 새로고침
                  pictureAdapter.addItem(PictureItem(savedUri))
                  gridView.invalidateViews()
              }

              override fun onError(exception: ImageCaptureException) {
                  Log.d(TAG,"실패")
                  exception.printStackTrace()
                  onBackPressed()
              }
          })

      }
  ```

  + 셔터 애니메이션
    + 셔터 애니메이션 리스너

    ```kotlin
    private fun setCameraAnimationListener() {
          cameraAnimationListener = object : Animation.AnimationListener {
              override fun onAnimationStart(animation: Animation?) { }
              
              //애니메이션이 끝날때 셔터 애니메이션 출려 뷰를 비활성화 하고, 촬영 사진을 보여준다
              override fun onAnimationEnd(animation: Animation?) {
                  frameLayoutShutter.visibility = View.GONE
                  showCaptureImage()
              }
              override fun onAnimationRepeat(animation: Animation?) { }
          }
      }
    ```
  
    + 셔터 애니메이션 호출 파트 (CameraCapture함수 내부에서 호출)

    ```kotlin
    val animation = AnimationUtils.loadAnimation(this@galleryActivity, R.anim.camera_shutter)
    animation.setAnimationListener(cameraAnimationListener)
    frameLayoutShutter.animation = animation
    frameLayoutShutter.visibility = View.VISIBLE
    frameLayoutShutter.startAnimation(animation)
    ```

### PhoneBook

+ 검색
  + 검색창 변경 리스너

  ```java
  callText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

      //입력 이벤트가 발생할때 마다 새로고침 할 수 있도록 검색함수를 호출해준다
      @Override
      public void afterTextChanged(Editable editable) {
          String text = callText.getText().toString();
          search(text);
      }
  });
  ```

  + 검색 함수

  ```java
  public void search(String charText) {

      // 문자 입력시마다 리스트를 지우고 새로 뿌려준다.
      callList.clear();

      // 문자 입력이 없을때는 모든 데이터를 보여준다.
      if (charText.length() == 0) {
          callList.addAll(callList2);
      }
      // 문자 입력을 할때..
      else
      {
          // 리스트의 모든 데이터를 검색한다.
          for(int i = 0;i < callList2.size(); i++)
          {
              // arraylist의 모든 데이터에 입력받은 단어(charText)가 포함되어 있으면 true를 반환한다.
              if (callList2.get(i).getName().toLowerCase().contains(charText))
              {
                  // 검색된 데이터를 리스트에 추가한다.
                  callList.add(callList2.get(i));
              }
          }
      }
      // 리스트 데이터가 변경되었으므로 아답터를 갱신하여 검색된 데이터를 화면에 보여준다.
      callAdapter.notifyDataSetChanged();
  }
  ```

+ 상세 정보창

  ```java
  list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          
          //개별 항목 마다 클릭시 상세정보창 activity로 이동
          Intent intent = new Intent(PhoneActivity.this, CallActivity.class);
          intent.putExtra("POSITION", position);
          startActivity(intent);
      }
  });
  ```

### StopWatch

+ 타이머 핸들러

```java
Handler myTimer = new Handler(){
    public void handleMessage(Message msg){
        //해당 핸들러가 호출될때 마다 타이머 값을 수정해준다.
        myOutput.setText(getTimeOut());

        //해당 핸들러를 delay없이 호출
        myTimer.sendEmptyMessage(0);
    }
};
//현재시각 반환 함수
String getTimeOut(){
    long now = SystemClock.elapsedRealtime();
    long outTime = now - myBaseTime;

    String easy_outTime = String.format("%02d:%02d:%02d", outTime/1000 / 60, (outTime/1000)%60,(outTime%1000)/10);
    return easy_outTime;
}
```

### Object Recognition

+ 영상분석 리스너

```kotlin
imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->

    //카메라가 회전한 경우 원래대로 변환
    if (!::bitmapBuffer.isInitialized) {
        imageRotationDegrees = image.imageInfo.rotationDegrees
        bitmapBuffer = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
    }

    //이미지를 RGB로 변환후 bitmapBuffer(텐서플로우 분석함수의 매개변수)에 저장
    mage.use { converter.yuvToRgb(image.image!!, bitmapBuffer) }

    // Tensorflow를 통해 이미지 처리
    val tfImage =  tfImageProcessor.process(tfImageBuffer.apply { load(bitmapBuffer) })
    // 이미지 처리 예측 결고 반환
    val predictions = detector.predict(tfImage)

    // 예측 결과중 가장 확률이 높은 1개를 선택하여 예측 결과와 해당 물체 바운더리 박스 출력
    reportPrediction(predictions.maxByOrNull { it.score })

    // 이미지 처리 analyzer 파이프라인 속도 계산
    val frameCount = 10
    if (++frameCounter % frameCount == 0) {
        frameCounter = 0
        val now = System.currentTimeMillis()
        val delta = now - lastFpsTimestamp
        val fps = 1000 * frameCount.toFloat() / delta
        Log.d(TAG, "FPS: ${"%.02f".format(fps)}")
        lastFpsTimestamp = now
    }
})
```

카메라 프리뷰 부분은 위 코드와 동일

### MetaMong

+ 영상 인식 후 3D 오브젝트 배치 코드

```java
//얼굴 랜더링 한 augmented face redering 불러오기
@Override
public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
}

@Override
//얼굴 표면 인식 및 좌표 감지
public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

    try {
        //backgroudRenderer = 얼굴 전체 rendering 
        backgroundRenderer.createOnGlThread(/*context=*/ this);
        //얼굴을 obj파일로 3D모델을 만들어서 형상화
        //3D Object(메타몽) 배치
        augmentedFaceRenderer.createOnGlThread(this, "models/metamonghi.png");
        augmentedFaceRenderer.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);

        //꽃도 마찬가지로 렌더링
        rightEarObject.createOnGlThread(this, "models/forehead_right.obj", "models/flower.png");
        rightEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f);
        rightEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending);


    } catch (IOException e) {
        Log.e(TAG, "Failed to read an asset file", e);
    }
}
```

## 구현 결과

### 1. 갤러리

![사물인식](https://user-images.githubusercontent.com/42797090/148048994-4990a717-5859-4eef-853d-bbece363a500.gif)

### 2. 연락처

![연락처](https://user-images.githubusercontent.com/42797090/148048616-fd7f4d82-fc78-4430-9cfc-18c8149db546.gif)

### 3. 스톱워치

![스톱워치](https://user-images.githubusercontent.com/42797090/148048509-6554268a-972d-4f4f-b209-b5bb6bc0726f.gif)

### 4. 사물인식

![original](https://user-images.githubusercontent.com/42797090/148052241-64f54bb6-e2ee-4116-b7b7-a72e8e64d114.gif)

### 5. 메타몽

![메타몽](https://user-images.githubusercontent.com/42797090/148050451-0e851278-c0ae-4aa3-b72f-7da633e9f99b.gif)
