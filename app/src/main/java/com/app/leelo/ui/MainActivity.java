package com.app.leelo.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.app.leelo.R;
import com.app.leelo.databinding.ActivityMainBinding;
import com.app.leelo.util.ThemeManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();
        
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        if (savedInstanceState == null) {
            replaceFragment(new TextFragment());
        }

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.texts) {
                replaceFragment(new TextFragment());
            } else if (item.getItemId() == R.id.words) {
                replaceFragment(new WordsFragment());
            } else if (item.getItemId() == R.id.settings) {
                replaceFragment(new SettingFragment());
            }
            return true;
        });
    }

    public void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}

