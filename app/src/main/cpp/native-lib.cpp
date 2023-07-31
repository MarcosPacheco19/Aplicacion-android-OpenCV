#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#include <opencv2/video.hpp>
#include <opencv2/videoio.hpp>
#include <vector>
#include <android/bitmap.h>
#include <opencv2/objdetect.hpp>

using namespace cv;
using namespace std;

CascadeClassifier detectorCaras;
vector<Rect> faces;

double distance(Point pt1, Point pt2) {
    int dx = pt1.x - pt2.x;
    int dy = pt1.y - pt2.y;
    return sqrt(dx * dx + dy * dy);
}

void bitmapToMat(JNIEnv * env, jobject bitmap, cv::Mat &dst, jboolean needUnPremultiplyAlpha){
    AndroidBitmapInfo info;
    void* pixels = 0;
    try {
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        dst.create(info.height, info.width, CV_8UC4);
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(needUnPremultiplyAlpha) cvtColor(tmp, dst, cv::COLOR_mRGBA2RGBA);
            else tmp.copyTo(dst);
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            cvtColor(tmp, dst, cv::COLOR_BGR5652RGBA);
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        //jclass je = env->FindClass("org/opencv/core/CvException");
        jclass je = env->FindClass("java/lang/Exception");
        //if(!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nBitmapToMat}");
        return;
    }
}
void matToBitmap(JNIEnv * env, cv::Mat src, jobject bitmap, jboolean needPremultiplyAlpha) {
    AndroidBitmapInfo info;
    void* pixels = 0;
    try {
        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
                   info.format == ANDROID_BITMAP_FORMAT_RGB_565 );
        CV_Assert( src.dims == 2 && info.height == (uint32_t)src.rows && info.width == (uint32_t)src.cols );
        CV_Assert( src.type() == CV_8UC1 || src.type() == CV_8UC3 || src.type() == CV_8UC4 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        if( info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 )
        {
            cv::Mat tmp(info.height, info.width, CV_8UC4, pixels);
            if(src.type() == CV_8UC1)
            {
                cvtColor(src, tmp, cv::COLOR_GRAY2RGBA);
            } else if(src.type() == CV_8UC3){
                cvtColor(src, tmp, cv::COLOR_RGB2RGBA);
            } else if(src.type() == CV_8UC4){
                if(needPremultiplyAlpha) cvtColor(src, tmp, cv::COLOR_RGBA2mRGBA);
                else src.copyTo(tmp);
            }
        } else {
            // info.format == ANDROID_BITMAP_FORMAT_RGB_565
            cv::Mat tmp(info.height, info.width, CV_8UC2, pixels);
            if(src.type() == CV_8UC1)
            {
                cvtColor(src, tmp, cv::COLOR_GRAY2BGR565);
            } else if(src.type() == CV_8UC3){
                cvtColor(src, tmp, cv::COLOR_RGB2BGR565);
            } else if(src.type() == CV_8UC4){
                cvtColor(src, tmp, cv::COLOR_RGBA2BGR565);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
        return;
    } catch(const cv::Exception& e) {
        AndroidBitmap_unlockPixels(env, bitmap);
        //jclass je = env->FindClass("org/opencv/core/CvException");
        jclass je = env->FindClass("java/lang/Exception");
        //if(!je) je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, e.what());
        return;
    } catch (...) {
        AndroidBitmap_unlockPixels(env, bitmap);
        jclass je = env->FindClass("java/lang/Exception");
        env->ThrowNew(je, "Unknown exception in JNI code {nMatToBitmap}");
        return;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_ec_edu_ups_visionartificialintegrador_CameraActivity_aEscalaGrises(
        JNIEnv* env,
        jobject /* this */,
        jobject input,
        jobject output) {

    Mat src;
    bitmapToMat(env, input, src, false);
    Mat tmp;
    cvtColor(src, tmp, cv::COLOR_BGR2GRAY);
    matToBitmap(env, tmp, output, false);
}

extern  "C" JNIEXPORT void JNICALL
Java_ec_edu_ups_visionartificialintegrador_CameraActivity_aDetectorMovimiento(
        JNIEnv* env,
        jobject /* this */,
        jobject input,
        jobject output) {

    static Mat prevFrame;
    Mat src;
    bitmapToMat(env, input, src, false);
    Mat grayFrame;
    cvtColor(src, grayFrame, cv::COLOR_BGR2GRAY);
    // Verificar si es el primer fotograma
    if (prevFrame.empty()) {
        prevFrame = grayFrame.clone();
    }
    // Calcular la diferencia absoluta entre el fotograma actual y el anterior
    Mat diffFrame;
    absdiff(prevFrame, grayFrame, diffFrame);
    // Aplicar un umbral para obtener una imagen binaria que resalte las regiones en movimiento
    Mat thresholdFrame;
    threshold(diffFrame, thresholdFrame, 50, 255, cv::THRESH_BINARY);
    // Actualizar el fotograma anterior con el fotograma actual para el siguiente ciclo
    prevFrame = grayFrame.clone();
    matToBitmap(env, thresholdFrame, output, false);
}

extern  "C" JNIEXPORT void JNICALL
Java_ec_edu_ups_visionartificialintegrador_CameraActivity_aDetectorTriangulos(
        JNIEnv* env,
        jobject /* this */,
        jobject input,
        jobject output) {

    Mat src;
    Mat srcGray;
    Mat edges;
    Mat frameWithTriangles;
    bitmapToMat(env, input, src, false);
    cvtColor(src,  srcGray, COLOR_BGR2GRAY);
    Canny(srcGray,edges,100,200);

    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    findContours(edges, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    frameWithTriangles = src.clone();
    for (size_t i = 0; i < contours.size(); i++) {
        vector<Point> approx;
        approxPolyDP(contours[i], approx, arcLength(contours[i], true) * 0.04, true);
        if (approx.size() == 3) {
            // Verificar si es un triángulo equilátero o isósceles
            double side1 = distance(approx[0], approx[1]);
            double side2 = distance(approx[1], approx[2]);
            double side3 = distance(approx[2], approx[0]);
            double ratio = max({side1, side2, side3}) / min({side1, side2, side3});

            if (ratio < 1.2) {
                // Triángulo equilátero
                fillPoly(frameWithTriangles, {approx}, Scalar(0, 255, 0));
            } else {
                // Triángulo isósceles
                fillPoly(frameWithTriangles, {approx}, Scalar(255, 0, 0));
            }
        }
    }

    matToBitmap(env, frameWithTriangles, output, false);
}



extern  "C" JNIEXPORT void JNICALL
Java_ec_edu_ups_visionartificialintegrador_CameraActivity_aDetectorSilueta(
        JNIEnv* env,
        jobject /* this */,
        jobject input,
        jobject output) {
    Mat src;
    Mat edges;
    Mat frameWithGreenBackground;
    Mat previousFrame;

    bitmapToMat(env, input, src, false);

    Canny(src, edges, 100, 200);
    src.copyTo(frameWithGreenBackground);
    frameWithGreenBackground.setTo(Scalar(0, 0, 0), edges);

    if (!previousFrame.empty()) {
        addWeighted(frameWithGreenBackground, 0.5, previousFrame, 0.5, 0, frameWithGreenBackground);
    }

    Mat dilatedEdges;
    dilate(edges, dilatedEdges, Mat(), Point(-1, -1), 1);
    frameWithGreenBackground.setTo(Scalar(0, 0, 0), dilatedEdges);

    // Actualizar previousFrame para la siguiente iteración
    previousFrame = frameWithGreenBackground.clone();

    // Convertir la matriz resultante a un objeto Bitmap de salida
    matToBitmap(env, frameWithGreenBackground, output, false);
}

extern  "C" JNIEXPORT void JNICALL
Java_ec_edu_ups_visionartificialintegrador_CameraActivity_aCapturarMovimiento(
        JNIEnv* env,
        jobject /* this */,
        jobject input,
        jobject output) {
    Mat src;
    bitmapToMat(env, input, src, false);

    Mat gray;
    cvtColor(src, gray, cv::COLOR_BGR2GRAY);

    GaussianBlur(gray, gray, Size(21, 21), 0);

    static Mat background = gray.clone();

    Mat diff;
    absdiff(background, gray, diff);

    Mat thresholded;
    threshold(diff, thresholded, 25, 255, THRESH_BINARY);

    Mat kernel = getStructuringElement(MORPH_RECT, Size(15, 15));
    morphologyEx(thresholded, thresholded, MORPH_OPEN, kernel);

    std::vector<std::vector<Point>> contours;
    findContours(thresholded, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    Rect armRect;
    for (const auto& contour : contours) {
        double area = contourArea(contour);
        if (area > 10000) {  // Ajustar el tamaño mínimo del brazo según sea necesario
            armRect = boundingRect(contour);
            break;
        }
    }

    rectangle(src, armRect, Scalar(0, 255, 0), 2);

    matToBitmap(env, src, output, false);
}

extern "C" JNIEXPORT void JNICALL
Java_ec_edu_ups_visionartificialintegrador_ExpresionesFaciales_detectarYDibujar(JNIEnv *env,
                                                     jobject /* this */,
                                                     jlong addrRgba) {
    Mat &frame = *(Mat *) addrRgba;
    Mat frame_gray;

    cvtColor( frame, frame_gray, COLOR_BGR2GRAY );
    equalizeHist( frame_gray, frame_gray );

    detectorCaras.detectMultiScale( frame_gray, faces);

    for ( size_t i = 0; i < faces.size(); i++ )
    {
        Point center( faces[i].x + faces[i].width/2, faces[i].y + faces[i].height/2 );
        rectangle( frame, Point( faces[i].x, faces[i].y ), Point( faces[i].x+faces[i].width, faces[i].y+faces[i].height), Scalar( 0, 255, 0 ), 2);

        Mat faceROI = frame_gray( faces[i] );
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_ec_edu_ups_visionartificialintegrador_ExpresionesFaciales_getTextoEmocion(JNIEnv *env, jobject /* this */, jfloat emotion_v) {
    string emotion_text;

    if(emotion_v>0 & emotion_v<0.5){
        emotion_text="Sorprendido";
    } else if(emotion_v>=0.5 & emotion_v<1.5){
        emotion_text="Miedo";
    } else if(emotion_v>=1.5 & emotion_v<2){
        emotion_text="Enojado/a";
    } else if(emotion_v>=2 & emotion_v<2.5) {
        emotion_text = "Neutral";
    } else if(emotion_v>=2.5 & emotion_v<3.5) {
        emotion_text = "Triste";
    } else if(emotion_v>=3.5 & emotion_v<5.5) {
        emotion_text = "Disgusto";
    } else {
        emotion_text = "Feliz";
    }

    return env->NewStringUTF(emotion_text.c_str());
}