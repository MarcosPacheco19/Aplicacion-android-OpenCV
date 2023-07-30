package ec.edu.ups.visionartificialintegrador;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

public class GalleryActivity extends AppCompatActivity {

    ViewPager pages;
    ArrayList<String> paths = new ArrayList<>();
    PagesAdapter pagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        File carpeta=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"/imagenesVision");

        createArray(carpeta);

        pages=(ViewPager) findViewById(R.id.viewpages);
        pagesAdapter=new PagesAdapter(GalleryActivity.this, paths);
        pages.setAdapter(pagesAdapter);
    }

    private void createArray(File carpeta){
        File list[] = carpeta.listFiles();
        if(list != null){
            for(File file: list){
                paths.add(file.getAbsolutePath());
            }
            /*for(int i=0; i<list.length;i++){
                paths.add(list[i].getAbsolutePath());
            }*/
        }
    }
}