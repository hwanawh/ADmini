package com.techtown.minipj;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatePicker datePicker;
    private TextView viewDatePick;
    private EditText edtDiary, edtPassword;
    private Button btnSave, btnPrevious, btnNext, btnPause, btnUnlock, btnLockUnlock;
    private List<Integer> songs;
    private MediaPlayer mediaPlayer;
    private int currentSongIndex = 0;
    private String fileName;
    private boolean isDiaryLocked = false; // 기본적으로 일기는 잠금 해제된 상태

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("내장메모리에 일기를 저장하고 읽을 수 있는 간단한 일기장 앱");

        datePicker = findViewById(R.id.datePicker);
        viewDatePick = findViewById(R.id.viewDatePick);
        edtDiary = findViewById(R.id.edtDiary);
        btnSave = findViewById(R.id.btnSave);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnPause = findViewById(R.id.btnPause);
        edtPassword = findViewById(R.id.edtPassword);
        btnUnlock = findViewById(R.id.btnUnlock);
        btnLockUnlock = findViewById(R.id.btnLockUnlock);

        // 기본적으로 현재 날짜의 일기를 체크합니다.
        Calendar c = Calendar.getInstance();
        int cYear = c.get(Calendar.YEAR);
        int cMonth = c.get(Calendar.MONTH);
        int cDay = c.get(Calendar.DAY_OF_MONTH);

        // 잠긴 상태에서는 일기를 보이지 않도록 처리
        if (!isDiaryLocked) {
            checkedDay(cYear, cMonth, cDay);
        }

        // 날짜 선택 시 이벤트 처리
        datePicker.init(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                checkedDay(year, monthOfYear, dayOfMonth);
            }
        });

        // 잠금/해제 버튼 클릭 시 이벤트 처리
        btnLockUnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLockUnlock();
            }
        });

        // 저장 버튼 클릭 시 이벤트 처리
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDiary(fileName);
            }
        });

        // 이전 버튼 클릭 시 이벤트 처리
        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });

        // 다음 버튼 클릭 시 이벤트 처리
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });

        // 일시정지 버튼 클릭 시 이벤트 처리
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePause();
            }
        });

        // 잠금 해제 버튼 클릭 시 이벤트 처리
        btnUnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPasswordDialog();
            }
        });

        // 음악 초기화 및 첫 번째 곡 재생
        initializeSongs();
        if (!isDiaryLocked) {
            playSong(currentSongIndex); // 잠금 해제 상태에서만 노래를 재생
        }
    }

    // 음악 초기화 메서드
    private void initializeSongs() {
        songs = new ArrayList<>();
        songs.add(R.raw.song1);
        songs.add(R.raw.song2);
        songs.add(R.raw.song3);
    }

    private void playSong(int songIndex) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, songs.get(songIndex));
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    private void playNext() {
        currentSongIndex = (currentSongIndex + 1) % songs.size();
        playSong(currentSongIndex);
    }

    private void playPrevious() {
        currentSongIndex = (currentSongIndex - 1 + songs.size()) % songs.size();
        playSong(currentSongIndex);
    }

    private void togglePause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPause.setText("▶ 재생");
            } else {
                mediaPlayer.start();
                btnPause.setText("▌▌ 일시정지");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void checkedDay(int year, int monthOfYear, int dayOfMonth) {
        viewDatePick.setText(year + " - " + (monthOfYear + 1) + " - " + dayOfMonth);
        fileName = year + "" + (monthOfYear + 1) + "" + dayOfMonth + ".txt";

        FileInputStream fis = null;
        try {
            fis = openFileInput(fileName);
            byte[] fileData = new byte[fis.available()];
            fis.read(fileData);
            fis.close();

            String str = new String(fileData, StandardCharsets.UTF_8);
            edtDiary.setText(str);
            btnSave.setText("수정하기");
        } catch (Exception e) {

            edtDiary.setText("");
            btnSave.setText("새 일기 저장");
            e.printStackTrace();
        }
    }

    private void saveDiary(String date) {
        String diaryText = edtDiary.getText().toString();
        try (FileOutputStream fos = openFileOutput(date, MODE_PRIVATE);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {
            writer.write(diaryText);
            Toast.makeText(getApplicationContext(), "일기 저장되었습니다.", Toast.LENGTH_SHORT).show();
            btnSave.setText("일기 수정");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "일기 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("비밀번호 입력");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = input.getText().toString().trim();
                if (password.equals("1234")) {
                    unlockDiary();
                } else {
                    Toast.makeText(MainActivity.this, "비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void toggleLockUnlock() {
        if (isDiaryLocked) {
            showPasswordDialog();
        } else {
            lockDiary();
        }
    }

    private void lockDiary() {
        isDiaryLocked = true;
        updateUIBasedOnLockStatus(isDiaryLocked);

    }

    private void unlockDiary() {
        isDiaryLocked = false;
        updateUIBasedOnLockStatus(isDiaryLocked);

        // 잠금 해제 후 현재 날짜의 일기를 다시 보여줍니다.
        Calendar c = Calendar.getInstance();
        int cYear = c.get(Calendar.YEAR);
        int cMonth = c.get(Calendar.MONTH);
        int cDay = c.get(Calendar.DAY_OF_MONTH);
        checkedDay(cYear, cMonth, cDay);
    }

    private void updateUIBasedOnLockStatus(boolean isLocked) {
        if (isLocked) {
            edtDiary.setEnabled(false); // 일기 내용 편집 창 비활성화
            btnSave.setEnabled(false); // 저장 버튼 비활성화
            btnPrevious.setEnabled(false); // 이전 버튼 비활성화
            btnNext.setEnabled(false); // 다음 버튼 비활성화
            btnPause.setEnabled(false); // 일시정지 버튼 비활성화

            // 비밀번호 입력 창과 잠금 해제 버튼을 보이도록 설정
            edtPassword.setVisibility(View.VISIBLE);
            btnUnlock.setVisibility(View.VISIBLE);

            // 잠금 상태 메시지 출력
            Toast.makeText(MainActivity.this, "일기가 잠겼습니다.", Toast.LENGTH_SHORT).show();

            // 일기 내용을 빈 문자열로 설정 (선택사항)
            edtDiary.setText("");
        } else {
            edtDiary.setEnabled(true); // 일기 내용 편집 창 활성화
            btnSave.setEnabled(true); // 저장 버튼 활성화
            btnPrevious.setEnabled(true); // 이전 버튼 활성화
            btnNext.setEnabled(true); // 다음 버튼 활성화
            btnPause.setEnabled(true); // 일시정지 버튼 활성화

            // 비밀번호 입력 창과 잠금 해제 버튼을 숨김
            edtPassword.setVisibility(View.GONE);
            btnUnlock.setVisibility(View.GONE);

            // 잠금 해제 상태 메시지 출력
            Toast.makeText(MainActivity.this, "일기가 잠금 해제되었습니다.", Toast.LENGTH_SHORT).show();

            // 잠금 해제 후 현재 날짜의 일기를 다시 보여줍니다.
            Calendar c = Calendar.getInstance();
            int cYear = c.get(Calendar.YEAR);
            int cMonth = c.get(Calendar.MONTH);
            int cDay = c.get(Calendar.DAY_OF_MONTH);
            checkedDay(cYear, cMonth, cDay);

        }
    }

}
