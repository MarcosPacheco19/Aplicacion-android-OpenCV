package ec.edu.ups.visionartificialintegrador;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import ec.edu.ups.visionartificialintegrador.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'visionartificialintegrador' library on application startup.
    static {
        System.loadLibrary("visionartificialintegrador");
    }

    private ActivityMainBinding binding;
    private Button camara;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        camara = findViewById(R.id.camara);
        camara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CameraActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP ));
            }
        });
    }
}