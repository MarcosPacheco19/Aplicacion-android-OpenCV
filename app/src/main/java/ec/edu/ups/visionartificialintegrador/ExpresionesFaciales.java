package ec.edu.ups.visionartificialintegrador;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ExpresionesFaciales  {

    private Interpreter interpreter;
    private int salida;
    private int altura=0;
    private int ancho=0;
    private CascadeClassifier cascadeClassifier;
    ExpresionesFaciales(AssetManager assets, Context context, String path, int inputSize) throws IOException {

        salida  = inputSize;
        Interpreter.Options options = new Interpreter.Options();

        options.setNumThreads(4);

        interpreter = new Interpreter(cargarModelo(assets, path), options);

        Log.d("Expresion facial","modelo ha sido cargado");

        try{
            InputStream is= context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);

            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File mArchivoCascada = new File(cascadeDir, "haarcascade_frontalface_alt");
            FileOutputStream os=new FileOutputStream(mArchivoCascada);

            byte[] buffer=new byte[4096];

            int leerByte;

            while((leerByte=is.read(buffer))!=-1){
                os.write(buffer, 0, leerByte);
            }

            is.close();
            os.close();

            cascadeClassifier=new CascadeClassifier(mArchivoCascada.getAbsolutePath());
            Log.d("Expresion Facial", "Clasificador cargado");


        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public Mat reconocerImagenes(Mat imagen){

        Core.flip(imagen.t(),imagen,1);
        Mat imagenGris=new Mat();
        Imgproc.cvtColor(imagen,imagenGris,Imgproc.COLOR_RGBA2GRAY);

        altura=imagenGris.height();
        ancho=imagenGris.width();

        int tamanioAbsoluto = (int)(altura*0.1);
        MatOfRect caras = new MatOfRect();

        if(cascadeClassifier != null){

            cascadeClassifier.detectMultiScale(imagenGris,caras,1.1,2,2,
                    new Size(tamanioAbsoluto,tamanioAbsoluto), new Size());

        }

        Rect[] caraArray = caras.toArray();

        for(int i=0; i<caraArray.length;i++){
            Imgproc.rectangle(imagen,caraArray[i].tl(),caraArray[i].br(), new Scalar(0,255,0,255),2);
            Rect roi=new Rect((int) caraArray[i].tl().x,(int) caraArray[i].tl().y,
                    ((int) caraArray[i].br().x)-(int)(caraArray[i].tl().x),
                    ((int) caraArray[i].br().y)-(int)(caraArray[i].tl().y));

            Mat cropped_rgba=new Mat(imagen,roi);
            Bitmap bitmap=null;
            bitmap=Bitmap.createBitmap(cropped_rgba.cols(),cropped_rgba.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgba,bitmap);
            Bitmap scaledBitmap=Bitmap.createScaledBitmap(bitmap,48,48,false);
            ByteBuffer byteBuffer=convertBitmapToByteBufer(scaledBitmap);
            float[][] emocion=new float[1][1];

            try {
                interpreter.run(byteBuffer, emocion);
            } catch (Exception e) {
                Log.e("Expresion facial", "Error: La ejecución en GPU falló, se usará CPU", e);
            }
            float emocion_v =(float)Array.get(Array.get(emocion,0),0);
            Log.d("Expresion facial","Salida:  "+ emocion_v);

            String emocionTexto = getTextoEmocion(emocion_v);

            Imgproc.putText(imagen,emocionTexto+"("+emocion_v+")",
                    new Point((int) caraArray[i].tl().x+10,(int) caraArray[i].tl().y+20),
                    1,1.5,new Scalar(0,0,255,150),2);
        }

        Core.flip(imagen.t(),imagen,0);
        return imagen;
    }


    private ByteBuffer convertBitmapToByteBufer(Bitmap scaledBitmap){
        ByteBuffer byteBuffer;
        int sizeImagen=salida;
        byteBuffer=ByteBuffer.allocateDirect(4*1*sizeImagen*sizeImagen*3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues=new int[sizeImagen*sizeImagen];

        scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),scaledBitmap.getHeight());
        int pixel=0;
        for(int i=0; i<sizeImagen; ++i){
            for(int j=0; j<sizeImagen; ++j){
                final int val=intValues[pixel++];
                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f);
                byteBuffer.putFloat((((val>>8)&0xFF))/255.0f);
                byteBuffer.putFloat(((val &0xFF))/255.0f);

            }
        }
        return byteBuffer;
    }
    private MappedByteBuffer cargarModelo(AssetManager assetManager, String path) throws IOException{
        AssetFileDescriptor assetFileDescriptor=assetManager.openFd(path);
        FileInputStream inputStream= new FileInputStream(assetFileDescriptor.getFileDescriptor());

        FileChannel fileChannel=inputStream.getChannel();

        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }

    public native void  detectarYDibujar(long addrRgba);
    public native String getTextoEmocion(float emotion_v);
}
