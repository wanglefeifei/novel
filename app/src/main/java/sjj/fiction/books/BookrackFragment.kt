package sjj.fiction.books

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import sjj.fiction.BaseFragment
import sjj.fiction.R

/**
 * Created by SJJ on 2017/10/7.
 */
class BookrackFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_books,container,false)
    }
}