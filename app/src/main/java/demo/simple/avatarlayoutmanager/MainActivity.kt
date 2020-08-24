package demo.simple.avatarlayoutmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import me.simple.alm.AvatarLayoutManager
import kotlin.random.Random

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        for (i in 0..3) {
            mItems.addAll(mImages)
        }

//        mItems.addAll(mImages)

        rv1.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true)
        rv1.adapter = AvatarAdapter()

        rv2.layoutManager = AvatarLayoutManager(AvatarLayoutManager.HORIZONTAL, false, 0, false)
        rv2.adapter = AvatarAdapter()
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