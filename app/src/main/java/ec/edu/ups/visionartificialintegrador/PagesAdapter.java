package ec.edu.ups.visionartificialintegrador;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.Objects;

public class PagesAdapter extends PagerAdapter {

    Context context;
    ArrayList<String> imagenes=new ArrayList<>();
    LayoutInflater layoutInflater;
    private VideoView video;
    private ImageView btnControl;
    private int reproducir=0;

    public  PagesAdapter(Context context, ArrayList imagenes){
        this.context=context;
        this.imagenes=imagenes;

        layoutInflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return imagenes.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view==((FrameLayout)object);
    }

    @Override
    public void destroyItem(ViewGroup container, int position,Object object) {
        container.removeView((FrameLayout) object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, final int position) {
        Uri uri=Uri.parse("file://"+imagenes.get(position));
        String[] STR=imagenes.get(position).split(".j",2);
        if(STR.length==2){
            View items=layoutInflater.inflate(R.layout.item,container,false);
            ImageView imagenesPaginas= items.findViewById(R.id.paginas);

            imagenesPaginas.setImageURI(uri);

            Objects.requireNonNull(container).addView(items);
            return items;
        }else {
            View videosItem = layoutInflater.inflate(R.layout.video_item, container, false);
            video=videosItem.findViewById(R.id.vistaVideo);
            btnControl=videosItem.findViewById(R.id.controlar);
            MediaController mediaController= new MediaController(this.context);
            mediaController.setAnchorView(video);

            video.setVideoURI(uri);
            video.requestFocus();
            btnControl.setImageResource(R.drawable.iniciar_video);
            video.start();
            reproducir=1;

            video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    btnControl.setImageResource(R.drawable.volver_grabar);
                    reproducir=0;
                }
            });

            btnControl.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        btnControl.setColorFilter(Color.DKGRAY);
                        return true;
                    }
                    if(event.getAction()==MotionEvent.ACTION_UP){
                        btnControl.setColorFilter(Color.WHITE);
                        if(reproducir==0){
                            btnControl.setImageResource(R.drawable.pausar_video);
                            reproducir=1;
                            video.start();
                        }else{
                            btnControl.setImageResource(R.drawable.iniciar_video);
                            reproducir=0;
                            video.pause();
                        }
                    }
                    return false;
                }
            });

            Objects.requireNonNull(container).addView(videosItem);
            return  videosItem;
        }
    }
}