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

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun showTime() {
        var apacheDiv = 0L
        var trueTimeDiv = 0L
        var tempoDiv = 0L
        Single.fromCallable{ NTPUDPClient().apply { defaultTimeout = 3000; open() } }
                .repeatWhen{ completed -> completed.delay(3, TimeUnit.SECONDS) }
                .map { client -> Pair(client.getTime(InetAddress.getByName("time.google.com")).message.transmitTimeStamp.date.time, Calendar.getInstance().timeInMillis) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (apacheTime, systemTime) ->
                    apacheDiv = apacheTime.minus(systemTime)
                }, { throwable -> throwable.printStackTrace() })

        Single.fromCallable { Pair(TrueTimeRx.now().time, Calendar.getInstance().timeInMillis) }
                .repeatWhen{ completed -> completed.delay(3, TimeUnit.SECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (trueTime, systemTime) ->
                    trueTimeDiv = trueTime.minus(systemTime)
                }, { throwable -> throwable.printStackTrace() })

        Single.fromCallable { Pair(Tempo.now(), Calendar.getInstance().timeInMillis) }
                .repeatWhen{ completed -> completed.delay(3, TimeUnit.SECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (tempoTime, systemTime) ->
                    tempoDiv = tempoTime?.minus(systemTime) ?: tempoDiv
                }, { throwable -> throwable.printStackTrace() })

        Single.fromCallable { Calendar.getInstance().timeInMillis }
                .repeat()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ androidTime ->
                    apache.text = "Apache: ${androidTime + apacheDiv}   Div: $apacheDiv"
                    truetime.text = "TrueTime: ${androidTime + trueTimeDiv}   Div: $trueTimeDiv"
                    tempo.text = "Tempo: ${androidTime + tempoDiv}   Div: $tempoDiv"
                    android.text = "System: $androidTime"
                }
    }
}
