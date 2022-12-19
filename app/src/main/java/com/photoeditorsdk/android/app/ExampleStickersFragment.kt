package com.photoeditorsdk.android.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.photoeditorsdk.android.app.StickerLoader.TARGET_SIZE
import kotlinx.coroutines.launch
import ly.img.android.pesdk.backend.decoder.ImageSource
import ly.img.android.pesdk.backend.model.config.ImageStickerAsset
import ly.img.android.pesdk.backend.model.state.layer.ImageStickerLayerSettings
import ly.img.android.pesdk.backend.model.state.layer.SpriteLayerSettings
import ly.img.android.pesdk.ui.sticker.custom.CustomStickersFragment

class ExampleStickersFragment : CustomStickersFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 5)

            lifecycleScope.launch {
                val items = DrawableAssetDatabase.getInstance(context).DrawableAssetDao().getAll()
                adapter = StickerAdapter(items) {
                    lifecycleScope.launch {
                        // Call onStickerSelected() on stickerSelectedListener and pass the ImageSource for the sticker to be added
                        stickerSelectedListener.onStickerSelected(
                            ImageSource.create(
                                StickerLoader.loadHiResSticker(
                                    context,
                                    it.drawableAssetId
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

class StickerAdapter(
    private val items: List<DrawableAsset>,
    private val onClick: (item: DrawableAsset) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder =
        StickerViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_sticker_view, parent, false)
        ) {
            if (it != RecyclerView.NO_POSITION) {
                onClick(items[it])
            }
        }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class StickerViewHolder(itemView: View, onClick: (position: Int) -> Unit): RecyclerView.ViewHolder(itemView) {
        private val stickerImageView = itemView.findViewById<ImageView>(R.id.stickerImageView)

        init {
            stickerImageView.setOnClickListener { onClick(bindingAdapterPosition) }
        }

        fun bind(asset: DrawableAsset) {
            val context = itemView.context
            Glide.with(itemView).load(
                context.resources.getIdentifier(
                    asset.drawableAssetId,
                    "drawable",
                    context.packageName
                )
            )
                .apply(
                    RequestOptions().override(
                        TARGET_SIZE,
                        TARGET_SIZE
                    )
                )
                .into(stickerImageView)
        }
    }
}