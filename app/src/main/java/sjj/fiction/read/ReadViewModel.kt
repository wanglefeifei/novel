package sjj.fiction.read

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import sjj.alog.Log
import sjj.fiction.data.repository.fictionDataRepository
import sjj.fiction.model.Chapter

class ReadViewModel(val name: String, val author: String) : ViewModel() {

    val book = fictionDataRepository.getBookInBookSource(name, author)

    fun getChapters(bookUrl: String) = fictionDataRepository.getChapters(bookUrl)

    fun loadChapter(chapter: Chapter): Observable<Chapter> {
        return fictionDataRepository.loadChapter(chapter)
    }

    fun getChapter(url: String): Observable<Chapter> {
        return fictionDataRepository.getChapter(url)
    }

    val readIndex = fictionDataRepository.getReadIndex(name, author)

    fun setReadIndex(index: Int): Observable<Int> {
        return fictionDataRepository.setReadIndex(name, author, index)
    }


    fun cachedBookChapter(bookUrl: String) = fictionDataRepository.cachedBookChapter(bookUrl)

}