package hu.hoc.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CouponsAdapter(
    private val coupons: List<Coupon>,
    private val onCopyClick: (Coupon) -> Unit,
    private val onStoreClick: (Coupon) -> Unit
) : RecyclerView.Adapter<CouponsAdapter.CouponViewHolder>() {
    
    class CouponViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.couponTitle)
        val store: TextView = view.findViewById(R.id.couponStore)
        val code: TextView = view.findViewById(R.id.couponCode)
        val discount: TextView = view.findViewById(R.id.couponDiscount)
        val copyButton: Button = view.findViewById(R.id.copyButton)
        val storeButton: Button = view.findViewById(R.id.storeButton)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coupon, parent, false)
        return CouponViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        val coupon = coupons[position]
        
        holder.title.text = coupon.title
        holder.store.text = coupon.store
        holder.code.text = coupon.code
        holder.discount.text = coupon.discount
        
        holder.copyButton.setOnClickListener { onCopyClick(coupon) }
        holder.storeButton.setOnClickListener { onStoreClick(coupon) }
    }
    
    override fun getItemCount() = coupons.size
}
