package ec.edu.ups.visionartificialintegrador;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CameraActivity extends org.opencv.android.CameraActivity {

    private Mat imagenRGB;
    private Mat imagenGris;
    private CameraBridgeViewBase cameraBridgeViewBase;
    private ImageView btnFlipCamera;
    private ImageView btnPhoto;
    private ImageView gallery;
    private ImageView filtrosCamara;
    private ImageView btnVideoCamera;

    private ImageView btnHistogram;

    private int camaraId=0;
    private int tomarPhoto=0;
    private int cambiarModo=0;
    private int verFiltros=0;
    private int tomarVideo=0;
    private int altura=0;
    private int ancho=0;
    private int mostarHistograma=0;
    private ListView filtros;
    private MediaRecorder mediaRecorder;
    private String filtroSeleccionado="";
    private Bitmap entrada;
    private Bitmap salida;
    private Bitmap histogramaImagen;
    private Bitmap entradaHistogram;
    private Bitmap salidaHistogram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, 101);
        }
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.RECORD_AUDIO}, 101);
        }
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }

        setContentView(R.layout.activity_camera);

        cameraBridgeViewBase = findViewById(R.id.frame);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                imagenRGB = new Mat(height,width, CvType.CV_8UC3);
                imagenGris = new Mat(height,width, CvType.CV_8UC1);
            }

            @Override
            public void onCameraViewStopped() {
                imagenRGB.release();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

                imagenRGB = inputFrame.rgba();

                if(camaraId==1){
                    Core.flip(imagenRGB,imagenRGB,1);
                }

                Mat nuevaImagen=new Mat();
                entrada = Bitmap.createBitmap(imagenRGB.cols(), imagenRGB.rows(), Bitmap.Config.ARGB_8888);
                salida = Bitmap.createBitmap(imagenRGB.cols(), imagenRGB.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(imagenRGB, entrada);

                if(Objects.equals(filtroSeleccionado, "Escala de grises")){
                    aEscalaGrises(entrada,salida);
                    Utils.bitmapToMat(salida, nuevaImagen);
                    imagenRGB=nuevaImagen.clone();
                } else if(Objects.equals(filtroSeleccionado, "Detecion de movimiento")){
                    aDetectorMovimiento(entrada,salida);
                    Utils.bitmapToMat(salida, nuevaImagen);
                    imagenRGB=nuevaImagen.clone();
                } else if (Objects.equals(filtroSeleccionado, "Sin Filtros")) {
                    imagenRGB=inputFrame.rgba();
                } else if(Objects.equals(filtroSeleccionado, "Deteccion de Silueta")){
                    aDetectorSilueta(entrada,salida);
                    Utils.bitmapToMat(salida, nuevaImagen);
                    imagenRGB=nuevaImagen.clone();
                } else if(Objects.equals(filtroSeleccionado, "Deteccion de Triangulos")){
                    aDetectorTriangulos(entrada,salida);
                    Utils.bitmapToMat(salida, nuevaImagen);
                    imagenRGB=nuevaImagen.clone();
                } else if(Objects.equals(filtroSeleccionado, "Capturar Movimientos")){
                    aCapturarMovimiento(entrada,salida);
                    Utils.bitmapToMat(salida, nuevaImagen);
                    imagenRGB=nuevaImagen.clone();
                }

                tomarPhoto=tomarFotografia(tomarPhoto,imagenRGB);
                mostarHistograma=mostrarHistograma(mostarHistograma,imagenRGB);

                altura=imagenRGB.height();
                ancho=imagenRGB.width();
                return imagenRGB;
            }
        });



        if(OpenCVLoader.initDebug()){
            cameraBridgeViewBase.enableView();
            Toast.makeText(getApplicationContext(), "OpenCV cargado satisfactoriamente", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), "OpenCV no se ha podido cargar", Toast.LENGTH_SHORT).show();
        }

        //Botones para la camara
        btnFlipCamera=findViewById(R.id.flip_camera);
        btnPhoto=findViewById(R.id.photo);
        gallery=findViewById(R.id.galeria);
        filtrosCamara=findViewById(R.id.filter);
        filtros=findViewById(R.id.filtros);
        mediaRecorder= new MediaRecorder();
        btnVideoCamera=findViewById(R.id.video_camera);
        btnHistogram=findViewById(R.id.btnHistogram);


        btnFlipCamera.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    btnFlipCamera.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP){
                    btnFlipCamera.setColorFilter(Color.WHITE);
                    voltearCamara();
                    return true;
                }
                return false;
            }
        });


        gallery.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    gallery.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP){
                    gallery.setColorFilter(Color.WHITE);
                    startActivity(new Intent(CameraActivity.this, GalleryActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP ));
                    return true;
                }
                return false;
            }
        });

        List<String> opcionesFiltros = new ArrayList<>();
        opcionesFiltros.add("Sin filtros");
        opcionesFiltros.add("Escala de grises");
        opcionesFiltros.add("Detecion de movimiento");
        opcionesFiltros.add("Deteccion de Silueta");
        opcionesFiltros.add("Deteccion de Triangulos");
        opcionesFiltros.add("Capturar Movimientos");

        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(this, R.layout.filtros_item, R.id.nombresFiltros,opcionesFiltros);
        String nullArray[]={};
        ArrayAdapter<String> nullArrayAdapter=new ArrayAdapter<>(this, R.layout.filtros_item, R.id.nombresFiltros,nullArray);
        filtros.setAdapter(nullArrayAdapter);


       filtrosCamara.setOnTouchListener(new View.OnTouchListener() {
           @Override
           public boolean onTouch(View v, MotionEvent event) {
               if(event.getAction()==MotionEvent.ACTION_DOWN){
                   filtrosCamara.setColorFilter(Color.DKGRAY);
                   return true;
               }
               if(event.getAction()==MotionEvent.ACTION_UP){
                   filtrosCamara.setColorFilter(Color.WHITE);
                   if(verFiltros == 0){
                       filtros.setAdapter(arrayAdapter);
                       verFiltros=1;
                   }else{
                       filtros.setAdapter(nullArrayAdapter);
                       verFiltros=0;
                   }
                   return true;
               }
               return false;
           }
       });

        filtros.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String opcionFiltro = opcionesFiltros.get(position);

                CameraActivity.this.filtroSeleccionado=opcionFiltro;

                filtros.setAdapter(nullArrayAdapter);
                verFiltros = 0;
            }
        });

       cameraBridgeViewBase.enableFpsMeter();

       btnVideoCamera.setOnTouchListener(new View.OnTouchListener() {
           @Override
           public boolean onTouch(View v, MotionEvent event) {
               if(event.getAction()==MotionEvent.ACTION_DOWN){
                   btnVideoCamera.setColorFilter(Color.DKGRAY);
                   return true;
               }
               if(event.getAction()==MotionEvent.ACTION_UP){
                   btnVideoCamera.setColorFilter(Color.WHITE);
                   if(cambiarModo==0){
                       btnPhoto.setImageResource(R.drawable.camera_video);
                       btnPhoto.setColorFilter(Color.WHITE);
                       cambiarModo=1;
                   }else{
                       btnPhoto.setImageResource(R.drawable.camera_photo);
                       cambiarModo=0;
                   }
                   return true;
               }
               return false;
           }
       });

        btnPhoto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction()==MotionEvent.ACTION_DOWN){
                    if(cambiarModo==0){
                        if(tomarPhoto==0){
                            btnPhoto.setColorFilter(Color.DKGRAY);
                        }
                    }
                    return true;
                }
                if(event.getAction()==MotionEvent.ACTION_UP){
                    if(cambiarModo==1){
                        if(tomarVideo==0){
                            try{
                                File carpeta = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+"/imagenesVision");
                                boolean satisfactorio=true;
                                if(!carpeta.exists()){
                                    satisfactorio=carpeta.mkdirs();
                                }
                                btnPhoto.setImageResource(R.drawable.camera_video);
                                btnPhoto.setColorFilter(Color.RED);
                                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                                CamcorderProfile camcorderProfile= CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
                                mediaRecorder.setProfile(camcorderProfile);

                                SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                                String dateVideo=sdf.format(new Date());
                                String nombreArchivo=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+"/imagenesVision/"+dateVideo+".mp4";
                                mediaRecorder.setOutputFile(nombreArchivo);
                                mediaRecorder.setVideoSize(ancho, altura);

                                mediaRecorder.prepare();
                                cameraBridgeViewBase.setRecorder(mediaRecorder);
                                mediaRecorder.start();

                            }catch (IOException err){
                                err.printStackTrace();
                            }
                            tomarVideo=1;
                        }else{
                            btnPhoto.setImageResource(R.drawable.camera_video);
                            btnPhoto.setColorFilter(Color.WHITE);
                            cameraBridgeViewBase.setRecorder(null);
                            mediaRecorder.stop();
                            try{
                                Thread.sleep(1000);
                            }catch (InterruptedException err){
                                throw new RuntimeException(err);
                            }
                            tomarVideo=0;
                        }
                    }else{
                        btnPhoto.setColorFilter(Color.WHITE);
                        if(tomarPhoto==0){
                            tomarPhoto=1;
                        } else {
                            tomarPhoto=0;
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        btnHistogram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mostarHistograma==0){
                    mostarHistograma=1;
                }else{
                    mostarHistograma=0;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    private void voltearCamara(){
        camaraId=camaraId^1;
        cameraBridgeViewBase.disableView();
        cameraBridgeViewBase.setCameraIndex(camaraId);
        cameraBridgeViewBase.enableView();
    }

    private int tomarFotografia(int tomarPhoto, Mat imagen){
        if(tomarPhoto==1){
            Mat guardarFoto=new Mat();
            Core.flip(imagen.t(), guardarFoto, 1);
            Imgproc.cvtColor(guardarFoto,guardarFoto,Imgproc.COLOR_RGBA2BGRA);
            File carpeta=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+"/imagenesVision");
            boolean satisfactorio=true;
            if(!carpeta.exists()){
                satisfactorio=carpeta.mkdirs();
            }
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String datePhoto=sdf.format(new Date());
            String nombreArchico=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+"/imagenesVision/"+datePhoto+".jpg";
            Imgcodecs.imwrite(nombreArchico, guardarFoto);
            tomarPhoto=0;
        }
        return tomarPhoto;
    }

    private int mostrarHistograma(int mostarHistograma, Mat imagenHistograma){
        if(mostarHistograma==1){

            entradaHistogram = Bitmap.createBitmap(imagenHistograma.cols(), imagenHistograma.rows(), Bitmap.Config.ARGB_8888);
            salidaHistogram = Bitmap.createBitmap(imagenHistograma.cols(), imagenHistograma.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(imagenHistograma, entradaHistogram);

            aHistograma(entradaHistogram,salidaHistogram);
            ImageView imageView = findViewById(R.id.imageViewHistogram);
            imageView.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));
            imageView.setImageBitmap(salidaHistogram);

            // Obtener referencia al RelativeLayout
            RelativeLayout relativeLayout = findViewById(R.id.layoutHistogram);

            // Agregar el ImageView al RelativeLayout
            relativeLayout.addView(imageView);
            imageView.setVisibility(View.VISIBLE);

            mostarHistograma=0;
        }
        return mostarHistograma;
    }

    public native void aEscalaGrises(Bitmap in, Bitmap out);
    public native void aDetectorMovimiento(Bitmap in, Bitmap out);
    public native void aDetectorTriangulos(Bitmap in, Bitmap out);
    public  native void aHistograma(Bitmap in, Bitmap out);
    public  native void aDetectorSilueta(Bitmap in, Bitmap out);
    public  native void aCapturarMovimiento(Bitmap in, Bitmap out);
}