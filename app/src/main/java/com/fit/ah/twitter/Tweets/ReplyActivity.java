package com.fit.ah.twitter.Tweets;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fit.ah.twitter.api.UsersApi;
import com.fit.ah.twitter.helper.MyApp;
import com.fit.ah.twitter.PostReply;
import com.fit.ah.twitter.R;
import com.fit.ah.twitter.ResponseVM;
import com.fit.ah.twitter.api.TweetApi;
import com.fit.ah.twitter.helper.MyBitmapConverter;
import com.fit.ah.twitter.helper.MyRunnable;
import com.fit.ah.twitter.helper.MySession;

import java.io.IOException;

public class ReplyActivity extends AppCompatActivity {

    private static final int OPEN_DOCUMENT_CODE = 2;
    Toolbar toolbar;
    EditText tweetContent;
    Button btnTweet;
    ImageView imgProfile, tweetImagePreview, btnImgTweet;
    TextView tweetLength, replyingToUsername, tweetImageRemove;
    LinearLayout replyPart;
    String tweetID;
    Bitmap tweetImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet);

        tweetContent = findViewById(R.id.tweetContent);
        btnTweet = findViewById(R.id.btnTweet);
        tweetLength = findViewById(R.id.tweetLength);
        toolbar = findViewById(R.id.nav_actionbar_tweet);
        replyingToUsername = findViewById(R.id.replyingToUsername);
        replyPart = findViewById(R.id.replyPart);
        imgProfile = findViewById(R.id.imgProfile);
        tweetImagePreview = findViewById(R.id.tweetImage);
        btnImgTweet = findViewById(R.id.tweetImgProfile);
        tweetImageRemove = findViewById(R.id.tweetImageRemove);

        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.close_icon);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        replyPart.setVisibility(View.VISIBLE);

        replyingToUsername.setText("@"+getIntent().getStringExtra("username"));
        tweetID = getIntent().getStringExtra("tweetID");

        UsersApi.GetProfileImg(this, new MyRunnable<String>() {
            @Override
            public void run(String result) {
                if (result != null)
                    imgProfile.setImageBitmap(MyBitmapConverter.StringToBitmap(result));
                else
                    imgProfile.setImageResource(R.mipmap.ic_launcher_round);
            }
        }, MySession.logiraniKorisnik.userID);

        tweetContent.addTextChangedListener(new TextWatcher() {
            CharSequence seq;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                if(s.length() > 140){
                    btnTweet.setEnabled(false);
                    btnTweet.setBackground(getResources().getDrawable(R.drawable.rounded_button_disabled));
                }
                else {
                    if(s.length() == 0 && tweetImagePreview.getVisibility() == View.GONE){
                        btnTweet.setEnabled(false);
                        btnTweet.setBackground(getResources().getDrawable(R.drawable.rounded_button_disabled));
                    }
                    else {
                        btnTweet.setEnabled(true);
                        btnTweet.setBackground(getResources().getDrawable(R.drawable.rounded_button));
                    }
                }
                seq = s;
            }

            private void setTweetLength(int count) {
                tweetLength.setText(String.valueOf(140-count));
                if(count > 140)
                    tweetLength.setTextColor(Color.RED);
                else
                    tweetLength.setTextColor(Color.WHITE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                setTweetLength(seq.length());
            }
        });

        btnImgTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, OPEN_DOCUMENT_CODE);
            }
        });

        tweetImageRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tweetImageRemove.setVisibility(View.GONE);
                tweetImagePreview.setVisibility(View.GONE);
                tweetImage = null;
                if(Integer.decode(tweetLength.getText().toString()) == 140){
                    btnTweet.setEnabled(false);
                    btnTweet.setBackground(getResources().getDrawable(R.drawable.rounded_button_disabled));
                }
            }
        });

        btnTweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String encodedImg = null;
                try{
                    encodedImg = MyBitmapConverter.BitmapToString(tweetImage);
                }
                catch (Exception e){
                }

                PostReply obj = new PostReply();
                obj.tweetContent = tweetContent.getText().toString();
                obj.tweetID = Integer.parseInt(tweetID);
                obj.userID = MySession.logiraniKorisnik.userID;
                obj.image = encodedImg;

                TweetApi.PostReply(ReplyActivity.this, new MyRunnable<ResponseVM>() {
                    @Override
                    public void run(ResponseVM result) {
                        if(result.responseCode == 200){
                            finish();
                            Toast.makeText(MyApp.getContext(), result.responseMessage, Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(MyApp.getContext(), result.responseMessage, Toast.LENGTH_SHORT).show();
                    }
                }, obj);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OPEN_DOCUMENT_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    bitmap = MyBitmapConverter.resize(bitmap);
                    tweetImagePreview.setImageBitmap(bitmap);
                    tweetImageRemove.setVisibility(View.VISIBLE);
                    tweetImagePreview.setVisibility(View.VISIBLE);
                    tweetImage = bitmap;
                    if(Integer.decode(tweetLength.getText().toString()) > -1) {
                        btnTweet.setEnabled(true);
                        btnTweet.setBackground(getResources().getDrawable(R.drawable.rounded_button));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getWindow().setStatusBarColor(getResources().getColor(R.color.statusbar_color));

        return true;
    }
}
