package sjj.fiction.data.source.remote.yunlaige

import io.reactivex.Observable
import org.jsoup.Jsoup
import sjj.fiction.App
import sjj.fiction.data.Repository.FictionDataRepository
import sjj.fiction.data.source.remote.HttpDataSource
import sjj.fiction.data.source.remote.HttpInterface
import sjj.fiction.model.*
import sjj.fiction.util.domain
import java.net.URLEncoder

/**
 * Created by SJJ on 2017/10/11.
 */
class YunlaigeDataSource : HttpDataSource(), FictionDataRepository.RemoteSource {
    private val service = create<HttpInterface>()

    override fun domain() = baseUrl().domain()
    override fun search(search: String): Observable<List<Book>> {
        val url = "http://www.yunlaige.com/modules/article/search.php"
        val encode = URLEncoder.encode(search, "gbk");
        return service.searchForGBK(url, mapOf("searchkey" to encode)).map {
            try {
                Jsoup.parse(it).body().getElementsByClass("chart-dashed-list")[0].children().map {
                    val child1 = it.child(1).child(0).child(0).select("a[href]")[0]
                    Book(child1.attr("href"), child1.text(), it.child(1).child(1).text().split("/")[0])
                }
            } catch (e: Exception) {
                val element = Jsoup.parse(it).body().getElementsByClass("book-info")[0]
                val info = element.getElementsByClass("info")[0]
                val name = info.child(0).child(0).text()
                val author = info.child(1).child(0).text()
                listOf(Book(App.app.config.getHttp302Url(url, "searchkey=$encode"), name, author))
            }
        }
    }

    override fun loadBookDetailsAndChapter(book: Book): Observable<Book> {
        return service.loadHtmlForGBK(book.url).map {
            //            val name: String, val author: String, val coverImgUrl: Url, val intro: String, val latestChapter: Chapter, val chapterList: List<Chapter>
            val element = Jsoup.parse(it).body().getElementsByClass("book-info")[0]
            val info = element.getElementsByClass("info")[0]
            book.bookCoverImgUrl = element.select("a[href]")[0].attr("href")
            book.intro = info.child(2).text()
            book.chapterListUrl = info.child(3).select("a[href]")[0].attr("href")
            book
        }.flatMap {
            loadChapterList(it)
        }
    }

    private fun loadChapterList(book: Book): Observable<Book> {
        return service.loadHtmlForGBK(book.chapterListUrl).map {
            book.chapterList = Jsoup.parse(it, book.chapterListUrl).getElementById("contenttable").child(0).select("a[href]").mapIndexed { index, e ->
                Chapter(e.attr("abs:href"), book.id, index, e.text())
            }
            book
        }
    }


    override fun loadBookChapter(chapter: Chapter): Observable<Chapter> {
        return service.loadHtmlForGBK(chapter.url).map {
            val element = Jsoup.parse(it).getElementById("content")
            chapter.content =element.html()
            chapter.isLoadSuccess = true
            chapter
        }
    }

    override fun baseUrl(): String = "http://www.yunlaige.com"
}