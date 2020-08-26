package demo.simple.stacklayoutmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import me.simple.lm.StackLayoutManager

class MainActivity : AppCompatActivity() {

    private val mItems = mutableListOf<Int>()
    private val mImages = mutableListOf<Int>(
        R.drawable.iu_1,
        R.drawable.iu_2,
        R.drawable.iu_3,
        R.drawable.iu_4,
        R.drawable.iu_5,
        R.drawable.iu_6
    )

    private val mViews by lazy { mutableListOf(rv1, rv2, rv3, rv4, rv5) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        for (i in 0..4) {
            mItems.addAll(mImages)
        }

//        mItems.addAll(mImages)

        rv1.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv1.adapter = AvatarAdapter()

        rv2.layoutManager = StackLayoutManager(StackLayoutManager.HORIZONTAL, false, 0, false)
        rv2.adapter = AvatarAdapter()

        rv3.layoutManager = StackLayoutManager(StackLayoutManager.HORIZONTAL, true, 20, false)
        rv3.adapter = AvatarAdapter()
//
        rv4.layoutManager = StackLayoutManager(StackLayoutManager.VERTICAL, false, 0, false)
        rv4.adapter = AvatarAdapter()
//
        rv5.layoutManager = StackLayoutManager(StackLayoutManager.VERTICAL, true, 20, true)
        rv5.adapter = AvatarAdapter()
    }

    private fun scrollTo(position: Int) {
        for (view in mViews) {
            view.scrollToPosition(position)
        }
    }

    private fun smoothScrollTo(position: Int) {
        for (view in mViews) {
            view.smoothScrollToPosition(position)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scroll -> {
                showToPositionDialog()
            }
            R.id.menu_setting -> {
                showSettingDialog()
            }
        }
        return true
    }

    private fun showSettingDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_setting)
            .show()
    }

    private fun showToPositionDialog() {
        val dialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_to_position)
            .show()
        val etToPosition = dialog.findViewById<EditText>(R.id.etToPosition)!!
        val btnToPosition = dialog.findViewById<Button>(R.id.btnToPosition)!!
        val btnSmoothToPosition = dialog.findViewById<Button>(R.id.btnSmoothToPosition)!!

        btnToPosition.setOnClickListener {
            dialog.dismiss()
            kotlin.runCatching {
                val position = etToPosition.text.toString().toInt()
                scrollTo(position)
            }
        }
        btnSmoothToPosition.setOnClickListener {
            dialog.dismiss()
            kotlin.runCatching {
                val position = etToPosition.text.toString().toInt()
                smoothScrollTo(position)
            }
        }
    }

    inner class AvatarAdapter : RecyclerView.Adapter<AvatarViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
            return AvatarViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_avatar, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return mItems.size
        }

        override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
            holder.imageView.setImageResource(mItems[position])
            holder.textView.text = position.toString()
        }
    }

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView = itemView.findViewById<ImageView>(R.id.ivAvatar)
        val textView = itemView.findViewById<TextView>(R.id.tvPosition)
    }
}