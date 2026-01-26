package hu.hoc.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ArticlesAdapter(
    private val articles: List<Article>,
    private val onItemClick: (Article) -> Unit
) : RecyclerView.Adapter<ArticlesAdapter.ArticleViewHolder>() {
    
    class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.articleImage)
        val title: TextView = view.findViewById(R.id.articleTitle)
        val excerpt: TextView = view.findViewById(R.id.articleExcerpt)
        val date: TextView = view.findViewById(R.id.articleDate)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        
        holder.title.text = article.title
        holder.excerpt.text = article.excerpt
        holder.date.text = article.date
        
        if (!article.imageUrl.isNullOrEmpty()) {
            holder.image.load(article.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }
            holder.image.visibility = View.VISIBLE
        } else {
            holder.image.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener { onItemClick(article) }
    }
    
    override fun getItemCount() = articles.size
}
