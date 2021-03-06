package sjj.novel.view.fragment


import androidx.databinding.DataBindingUtil
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_choose_book_source.*
import kotlinx.android.synthetic.main.item_details_book_source_list.view.*
import sjj.novel.BaseFragment
import sjj.novel.R
import sjj.novel.databinding.FragmentChooseBookSourceBinding
import sjj.novel.util.getModel
import sjj.novel.util.observeOnMain
import sjj.novel.util.requestOptions
import sjj.novel.view.adapter.BaseAdapter


/**
 *书籍换源 刷新
 */
class ChooseBookSourceFragment : BaseFragment() {

    companion object {
        const val BOOK_NAME = "BOOK_NAME"
        const val BOOK_AUTHOR = "BOOK_AUTHOR"

        fun newInstance(name: String, author: String): ChooseBookSourceFragment {
            return ChooseBookSourceFragment().apply {
                arguments = Bundle().apply {
                    putString(BOOK_NAME, name)
                    putString(BOOK_AUTHOR, author)
                }
            }
        }

    }

    private lateinit var model: ChooseBookSourceViewModel
    private val adapter = ChooseBookSourceAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        model = getModel {
            arrayOf(arguments!!.getString(BOOK_NAME)!!, arguments!!.getString(BOOK_AUTHOR)!!)
        }

        val binding = DataBindingUtil.inflate<FragmentChooseBookSourceBinding>(inflater, R.layout.fragment_choose_book_source, container, false)
        binding.model = model
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter.setHasStableIds(true)
        books.adapter = adapter
        model.fillViewModel().observeOnMain().subscribe {
            adapter.data = it
            adapter.notifyDataSetChanged()
        }.destroy("ChooseBookSourceFragment fill view model")
        swipe.setOnRefreshListener {
            swipe.isRefreshing = false
            model.refresh().subscribe().destroy("ChooseBookSourceFragment refresh book")
        }
    }

    private inner class ChooseBookSourceAdapter : BaseAdapter() {
        var data = listOf<ChooseBookSourceViewModel.ChooseBookSourceItemViewModel>()
        override fun getItemCount(): Int = data.size

        override fun itemLayoutRes(viewType: Int): Int {
            return R.layout.item_details_book_source_list
        }

        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
            val bind = DataBindingUtil.bind<sjj.novel.databinding.ItemDetailsBookSourceListBinding>(holder.itemView)
            val bookGroup = data[position]
            bind?.model = bookGroup

            bookGroup.bookCover.onBackpressureLatest().observeOnMain().subscribe {
                Glide.with(this@ChooseBookSourceFragment)
                        .applyDefaultRequestOptions(requestOptions)
                        .load(it)
                        .into(holder.itemView.bookCover)
            }.destroy("fragment shelf book cover ${bookGroup.id}")

            holder.itemView.setOnClickListener { _ ->
                model.setBookSource(bookGroup.book).observeOnMain().subscribe {
                    dismiss()
                }.destroy("set novel source")
            }
        }

        override fun getItemId(position: Int): Long {
            return data[position].id
        }

    }

}
