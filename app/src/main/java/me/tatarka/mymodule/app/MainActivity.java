package me.tatarka.mymodule.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.tatarka.fittextview.lib.FitTextView;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView fontSample = (TextView) findViewById(R.id.font_sample);
        Typeface font = Typeface.createFromAsset(getAssets(), "Once_upon_a_time.ttf");
        fontSample.setTypeface(font);

        ListView list = (ListView) findViewById(R.id.list);
        ArrayList<String> data = new ArrayList<String>();
        Random random = new Random();
        String alphabet = "  abcdefghijklmnopqrstuvwxyz  ";

        for (int i = 0; i < 100; i++) {
            String gobalygook = "";
            int length = random.nextInt(40);
            for (int j = 0; j < length; j++)  {
                gobalygook = gobalygook + alphabet.charAt(random.nextInt(alphabet.length()));
            }
            data.add("This text will always be on one line " + gobalygook);
        }
        list.setAdapter(new CachingFitTextAdapter(this, R.layout.list_item, data));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class CachingFitTextAdapter extends ArrayAdapter<String> {
        private FitTextView.Cache cache = new FitTextView.Cache();

        public CachingFitTextAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (convertView == null) {
                ((FitTextView) view).setTextSizeCache(cache);
            }
            return view;
        }
    }
}
