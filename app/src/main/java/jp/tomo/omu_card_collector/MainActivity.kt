package jp.tomo.omu_card_collector

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import jp.tomo.omu_card_collector.databinding.ActivityMainBinding
import java.security.AccessController.getContext
import java.util.Arrays
import java.util.Random


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private var nfcmanager : NfcManager? = null
    private var nfcadapter : NfcAdapter? = null
    private var callback : CustomReaderCallback? = null

    private var userList:MutableList<String> = mutableListOf()

    private var adapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle("OMU 学籍番号取集アプリ for TA")

        setSupportActionBar(binding.toolbar)

        nfcmanager = this.getSystemService(Context.NFC_SERVICE) as NfcManager?
        nfcadapter = nfcmanager!!.getDefaultAdapter()
        callback = CustomReaderCallback(this)
        nfcadapter!!.enableReaderMode(this,callback
            ,NfcAdapter.FLAG_READER_NFC_F,null)

        // ListViewにデータをセットする
        val list = findViewById<ListView>(R.id.listview)
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            userList
        )
        list.adapter = adapter

        // クリックイベント
        list.setOnItemLongClickListener { parent, view, position, id ->
            AlertDialog.Builder(this)
                .setTitle("Delete Confirmation")
                .setMessage("Is it ok to delete this data? ("+userList[position]+")")
                .setPositiveButton("Delete", DialogInterface.OnClickListener { dialog, which ->
                    userList.remove(userList[position])
                    adapter!!.notifyDataSetChanged()
                    dialog.dismiss()
                })
                .setNegativeButton("No", DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                .setNeutralButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                .show()

            true
        }

        findViewById<AppCompatButton>(R.id.reset_button).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Confirmation")
                .setMessage("Is it ok to clear all data?")
                .setPositiveButton("Clear", DialogInterface.OnClickListener { dialog, which ->
                    userList.clear()
                    adapter!!.notifyDataSetChanged()
                    dialog.dismiss()
                })
                .setNegativeButton("No", DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                .setNeutralButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                .show()
        }
    }

    // 00AJG2200100
    // 123140104000
    fun addStudentId(str:String){
        runOnUiThread {
            val resID = resources.getIdentifier("success", "raw", packageName)
            val mediaPlayer = MediaPlayer.create(this, resID)
            mediaPlayer.start()

            var studentId = str

            if(studentId.startsWith("00")){
                studentId = studentId.substring(2)
            }
            if(studentId.endsWith("00")){
                studentId = studentId.substring(0,studentId.length-2)
            }

            if(findViewById<SwitchCompat>(R.id.testmode_switch).isChecked){
                studentId = studentId.substring(0,studentId.length-3) + getRandomString()
            }
            Log.d("student_id", studentId)
            findViewById<TextView>(R.id.textview_first).text = studentId

            if(!userList.contains(studentId)){
                adapter!!.add(studentId)
                userList.plus(studentId)
                userList.sort()
                adapter!!.notifyDataSetChanged()
                //findViewById<ListView>(R.id.listview).adapter = adapter
            }
        }
    }

    protected fun getRandomString(): String? {
        val SALTCHARS = "1234567890" // ABCDEFGHIJKLMNOPQRSTUVWXYZ
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < 3) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        return salt.toString()
    }

    fun readError(message: String){
        runOnUiThread {
            val resID = resources.getIdentifier("error", "raw", packageName)
            val mediaPlayer = MediaPlayer.create(this, resID)
            mediaPlayer.start()
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            mediaPlayer.setOnCompletionListener(OnCompletionListener { mediaPlayer ->
                mediaPlayer.stop()
                mediaPlayer.release()
            })
        }
    }

    override fun onResume() {
        super.onResume()
        nfcadapter!!.enableReaderMode(this,callback
            ,NfcAdapter.FLAG_READER_NFC_F,null)
    }
    override fun onPause() {
        super.onPause()
        nfcadapter!!.disableReaderMode(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcadapter!!.disableReaderMode(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private class CustomReaderCallback(val activity:MainActivity) : NfcAdapter.ReaderCallback {
        private var handler : android.os.Handler? = null

        override fun onTagDiscovered(tag: Tag) {
            Log.d("Sample", tag.id.toString())
            try{
                val data = String(trim(NfcReader().readTag(tag)[0]))
                activity.addStudentId(data)
            } catch (e:Exception) {
                activity.readError(e.localizedMessage!!)
            }
        }

        fun trim(bytes: ByteArray): ByteArray {
            var i = bytes.size - 1
            while (i >= 0 && bytes[i].toInt() == 0) {
                --i
            }
            return Arrays.copyOf(bytes, i + 1)
        }

        private fun byteToHex(b : ByteArray) : String{
            var s : String = ""
            for (i in 0..b.size-1){
                s += "[%02X]".format(b[i])
            }
            return s
        }

        fun setHandler(handler  : android.os.Handler){
            this.handler = handler
        }
    }

}