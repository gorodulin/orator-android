package com.orator

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.instacart.library.truetime.TrueTimeRx
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.tempo.Tempo
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.net.ntp.NTPUDPClient
import java.util.*
import java.util.concurrent.TimeUnit
import java.net.InetAddress


class MainActivity : AppCompatActivity() {

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Tempo.initialize(application)

        TrueTimeRx.build()
                .initializeRx("time.google.com")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { date ->
                    Log.v("Hello", "TrueTime was initialized and we have a time: $date")
                    showTime()
                }
    }

    @SuppressLint("CheckResult")
    private fun showTime() {
        Single.fromCallable{ NTPUDPClient().apply { defaultTimeout = 3000; open() } }
                .repeatWhen{ completed -> completed.delay(100, TimeUnit.MILLISECONDS) }
                .map { client -> client.getTime(InetAddress.getByName("time.google.com")).message.transmitTimeStamp.date }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { apacheTime ->
                    apache.text = apacheTime.time.toString()
                }

        Single.fromCallable { TrueTimeRx.now().time }
                .repeatWhen{ completed -> completed.delay(100, TimeUnit.MILLISECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ truetimeDate->
                    truetime.text = truetimeDate.toString()
                }

        Single.fromCallable { Tempo.now() }
                .repeatWhen{ completed -> completed.delay(100, TimeUnit.MILLISECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ tempoDate ->
                    tempo.text = tempoDate.toString()
                }

        Single.fromCallable { Calendar.getInstance().timeInMillis }
                .repeatWhen{ completed -> completed.delay(100, TimeUnit.MILLISECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ androidDate ->
                    android.text = androidDate.toString()
                }
    }
}
