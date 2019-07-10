package mysqltest.app.mysqlconnnection2;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView textView ;
    TextView textView2 ;
    public static final String DB_URL = "jdbc:mysql://10.0.2.2/mywordpress";
    public static final String DB_URL1 = "jdbc:mysql://localhost:8080/erdogan?user=erdogan&password=123456&useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = "erdogan";
    private static final String PASS = "123456";
    DatabaseReference mDatabase;
    Boolean compareString2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();

        //mDatabase = FirebaseDatabase.getInstance().getReference();
        //mDatabase.child("users").child(uuidString).setValue("ddd");


        // OneSignal Initialization

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        final Handler handler = new Handler();
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);


        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{
                    Send objSend = new Send();
                    objSend.execute();

                    mDatabase = FirebaseDatabase.getInstance().getReference();
                    OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                        @Override
                        public void idsAvailable(final String userId, String registrationId) {
                            System.out.println("userid: "+userId);
                            UUID uuid = UUID.randomUUID();
                            final String uuidString = uuid.toString();
                            //her seferinde eklencek.aynı player istemediğimiz durum.
                            DatabaseReference newReference = FirebaseDatabase.getInstance().getReference("PlayerIds");
                            newReference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    ArrayList<String> playerIDsFromServer = new ArrayList<>();
                                    for (DataSnapshot ds : dataSnapshot.getChildren())
                                    {   //gelen data hasmap tarzında
                                        HashMap <String,String> hashMap = (HashMap<String, String>) ds.getValue();
                                        String currentPlayerID = hashMap.get("playerID");
                                        playerIDsFromServer.add(currentPlayerID);
                                        if (compareString2 == true){
                                            try {
                                                OneSignal.postNotification(new JSONObject("{'contents': {'en':'İçerik Güncellendi'}, 'include_player_ids': ['" + currentPlayerID + "']}"), null);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    if (!playerIDsFromServer.contains(userId)){
                                        mDatabase.child("PlayerIds").child(uuidString).child("playerID").setValue(userId);

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        }
                    });
                }
                catch (Exception e) {

                }
                finally{
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 2000);
                }
            }
        };
        handler.postDelayed(runnable, 2000);








    }
    public void getLast(View view){
        //button assyn task görevi yapcak arkada threadi yormucak uygulama kitlenmicek
        Send objSend = new Send();
        objSend.execute();

    }
    private class Send extends AsyncTask<String,String,String>
    {
        String msg = "";
        String text = textView.getText().toString();
        String text2;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(),"Lütfen Bekleyin Pre çalışıyor",Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... strings)
        {
            try{
                //burası bağlantı için
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                java.sql.Connection conn = DriverManager.getConnection(DB_URL,USER,PASS);
                if (conn == null){
                    msg = "Connection goes wrong boş bağlantı hatası";
                }else {
                    String query = "SELECT post_title FROM wp_posts WHERE id=212 ";
                    String query2 = "SELECT post_date FROM wp_posts ORDER BY ID DESC LIMIT 1";
                    String query3 = "SELECT post_title FROM wp_posts ORDER BY ID DESC LIMIT 1";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query3);
                    if (rs != null){
                        while(rs.next()){
                            try{
                                //tek sorgu dönüp onuda post date yazcak
                                text2 = rs.getString("post_title");

                                //mesajı göster
                                //Toast.makeText(getApplicationContext(),text2,Toast.LENGTH_LONG).show();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            msg = text2;
                        }
                    }else {
                        msg = "No Data Found";
                    }

                }

            }catch (Exception e){
                msg = "Connection error hata!!";
                e.printStackTrace();
                Writer writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                msg = writer.toString();
            }


            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String date = df.format(Calendar.getInstance().getTime());
            //boşluğa göre ayırdım. önce tarihi çekcem sonra saati eğer eşleşirse yenidir.
            String[] splitMsgArray =msg.split(" ");
            String dateMsgIndex = splitMsgArray[0];
            String clockMsgIndex = splitMsgArray[1];
            String[] splitCurrentArray = date.split(" ");
            String dateCurrIndex = splitMsgArray[0];
            String clockCurrIndex = splitMsgArray[1];
            //-------------hem current android saatin hemde database saatinin gerçek zamanını aldım
            //sırada databaseden gelen saati almakta çünkü ikisinin
            /*String[] clockMsgHourIndex = clockMsgIndex.split(":");
            String clockMsgHour = clockMsgHourIndex[0];
            String clockMsgMin = clockMsgHourIndex[1];
            String clockCombineTime = clockMsgHour+":"+clockMsgMin;*/
            textView.setText(msg);
            //Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
            //Toast.makeText(getApplicationContext(),date,Toast.LENGTH_LONG).show();
            //Toast.makeText(getApplicationContext(),clockCombineTime,Toast.LENGTH_SHORT).show();
            Boolean compareString = dateMsgIndex.equals(dateCurrIndex);
            //Toast.makeText(getApplicationContext(),compareString.toString(),Toast.LENGTH_LONG).show();
            String myString = "merhaba erdo";
            compareString2 = msg.equals(myString);
            Toast.makeText(getApplicationContext(),compareString2.toString(),Toast.LENGTH_LONG).show();
            textView2.setText(date);
        }
    }
}
