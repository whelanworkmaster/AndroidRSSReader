package com.example.whelan.rssapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button mBtnFetch;
    private EditText mEditTextFetch;
    private TextView mTextViewTitle;
    private TextView mTextViewDesc;
    private TextView mTextViewLink;

    private List<RssFeedModel> mFeedModelList;
    private String mFeedTitle;
    private String mFeedDesc;
    private String mFeedLink;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnFetch = (Button) findViewById(R.id.btnFetch);
        mEditTextFetch = (EditText) findViewById(R.id.editTextFetch);
        mTextViewTitle = (TextView) findViewById(R.id.textViewTitle);
        mTextViewDesc = (TextView) findViewById(R.id.textViewDesc);
        mTextViewLink = (TextView) findViewById(R.id.textViewLink);

        mBtnFetch.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                new FetchFeedTask().execute((Void) null);
            }
        });
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {

        private String urlLink;

        @Override
        protected void onPreExecute() {
            urlLink = mEditTextFetch.getText().toString();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if(TextUtils.isEmpty(urlLink)) {
                return false;
            }

            try {
                // To check if the input has the correct url formattingd
                if(!urlLink.startsWith("http://") && !urlLink.startsWith("https://")) {
                    urlLink = "http://" + urlLink;
                }
                URL url = new URL(urlLink);
                InputStream inputStream = url.openConnection().getInputStream();
                mFeedModelList = parseFeed(inputStream);
                return true;
            } catch(IOException e) {
                Log.e(TAG, "Error", e);
            } catch(XmlPullParserException e) {
                Log.e(TAG, "Error", e);
            }

            return false;
        }

        // Only update UI in pre and post update
        @Override
        protected void onPostExecute(Boolean success) {

            if(success) {
                mTextViewTitle.setText("Feed Title: " + mFeedTitle);
                mTextViewDesc.setText("Feed Description: " + mFeedDesc);
                mTextViewLink.setText("Feed Link: " + mFeedLink);
            } else {
                Toast.makeText(MainActivity.this,
                        "Enter a valid Rss feed URL", Toast.LENGTH_LONG).show();
            }
        }
    }

    public List<RssFeedModel> parseFeed(InputStream inputStream) throws XmlPullParserException, IOException {

        String title = null;
        String desc = null;
        String link = null;
        boolean isItem = false;
        List<RssFeedModel> items = new ArrayList<>();

        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlPullParser.setInput(inputStream, null);

            xmlPullParser.nextTag();
            while(xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                int eventType = xmlPullParser.getEventType();

                String name = xmlPullParser.getName();
                if(name == null) {
                    continue;
                }

                if(eventType == XmlPullParser.END_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = false;
                    }
                    continue;
                }

                if(eventType == XmlPullParser.START_TAG) {
                    if(name.equalsIgnoreCase("item")) {
                        isItem = true;
                        continue;
                    }
                }

                Log.d("MyXMLParser", "Parsing Name ==> " + name);
                String result = "";
                if(xmlPullParser.next() == XmlPullParser.TEXT) {
                    result = xmlPullParser.getText();
                    xmlPullParser.nextTag();
                }

                // Here we actually check if the result is one of our wanted results, if so
                // we save it in our variables
                if(name.equalsIgnoreCase("title")) {
                    title = result;
                } else if(name.equalsIgnoreCase("link")) {
                    link = result;
                } else if(name.equalsIgnoreCase("description")) {
                    desc = result;
                }

                if(title != null && link != null && desc != null) {
                    if(isItem) {
                        RssFeedModel item = new RssFeedModel(title, link, desc);
                        items.add(item);
                    } else {
                        mFeedTitle = title;
                        mFeedLink = link;
                        mFeedDesc = desc;
                    }

                    title = null;
                    link = null;
                    desc = null;
                    isItem = false;
                }
            }

            return items;
        } finally {
            inputStream.close();
        }

    }

    public class RssFeedModel {

        private String title;
        private String link;
        private String desc;

        public RssFeedModel(String title, String link, String desc) {
            this.title = title;
            this.link = link;
            this.desc = desc;
        }
    }
}


