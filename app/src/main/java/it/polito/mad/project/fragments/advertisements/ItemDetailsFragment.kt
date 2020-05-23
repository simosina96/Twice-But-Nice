package it.polito.mad.project.fragments.advertisements

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging

import it.polito.mad.project.R
import it.polito.mad.project.commons.fragments.NotificationFragment
import it.polito.mad.project.fragments.profile.UserViewModel
import it.polito.mad.project.models.Item
import kotlinx.android.synthetic.main.fragment_item_details.*
import kotlinx.android.synthetic.main.fragment_item_details.item_descr
import kotlinx.android.synthetic.main.fragment_item_details.item_exp
import kotlinx.android.synthetic.main.fragment_item_details.item_location
import kotlinx.android.synthetic.main.fragment_item_details.item_photo
import kotlinx.android.synthetic.main.fragment_item_details.item_price
import kotlinx.android.synthetic.main.fragment_item_details.item_title
import kotlinx.android.synthetic.main.fragment_item_details.loadingLayout
import org.json.JSONObject

class ItemDetailsFragment : NotificationFragment() {

    private lateinit var itemViewModel: ItemViewModel
    private lateinit var userViewModel: UserViewModel

    private var isMyItem: Boolean = false

    private var listenerRegistration: ListenerRegistration? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemViewModel = ViewModelProvider(requireActivity()).get(ItemViewModel::class.java)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

    }

    override fun onStart() {
        super.onStart()
        (activity as AppCompatActivity?)?.supportActionBar?.show()

        isMyItem = arguments?.getBoolean("IsMyItem")?:false
        itemViewModel.item.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                item_title.text = it.title
                item_descr.text = it.description
                item_location.text = it.location
                item_category.text = "${it.category} - ${it.subcategory}"
                item_price.text = "${it.price} €"
                item_exp.text = it.expiryDate
                if (listenerRegistration == null)
                   listenerRegistration = itemViewModel.listenToChanges()
            }
        })

        itemViewModel.itemPhoto.observe(viewLifecycleOwner, Observer {
            if (it != null){
                item_photo.setImageBitmap(it)
            }
        })

        itemViewModel.loader.observe(viewLifecycleOwner, Observer {
            if (itemViewModel.isNotLoading()) {
                loadingLayout.visibility = View.GONE
                if (itemViewModel.error) {
                    Toast.makeText(context, "Error on item loading", Toast.LENGTH_SHORT).show()
                }
                if (!isMyItem) {
                    if (itemViewModel.item.value!!.status == "Available") {
                        var interestFabDrawableId: Int = R.drawable.ic_favorite_border_white_24dp
                        if (itemViewModel.itemInterest.interest)
                            interestFabDrawableId = R.drawable.ic_favorite_white_24dp
                        interestFab.setImageResource(interestFabDrawableId)
                        interestFab.show()
                    } else {
                        interestFab.hide()
                    }
                    interestedUsersFab.hide()
                } else {
                    interestFab.hide()
                    interestedUsersFab.show()
                }

            } else {
                loadingLayout.visibility = View.VISIBLE
                interestFab.hide()
                interestedUsersFab.hide()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_item_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFabButton()
        itemViewModel.loadItem(arguments?.getString("ItemId")!!)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (isMyItem) {
            inflater.inflate(R.menu.edit_menu, menu)
        }
    }

    override fun onOptionsItemSelected(option: MenuItem): Boolean {
        // Handle item selection
        return when (option.itemId) {
            R.id.pencil_option -> {
                var bundle = bundleOf("ItemId" to itemViewModel.item.value?.id)
                this.findNavController().navigate(R.id.action_showItemFragment_to_itemEditFragment, bundle)
                true
            }
            else -> super.onOptionsItemSelected(option)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }

    private fun setFabButton() {
        interestFab.setOnClickListener {
            itemViewModel.updateItemInterest()
                .addOnSuccessListener {
                    val item = itemViewModel.item.value as Item
                    val name = userViewModel.user.value!!.name
                    val body = JSONObject().put("ItemId", item.id).put("IsMyItem", true)
                    if (itemViewModel.itemInterest.interest) {
                        FirebaseMessaging.getInstance().subscribeToTopic(item.id!!)
                        sendNotification(item.user, item.title, "$name è interessato al tuo prodotto", body)
                    }
                    else {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(item.id!!)
                        sendNotification(item.user, item.title, "$name non è più interessato al tuo prodotto", body)
                    }
                }
        }
        interestedUsersFab.setOnClickListener{
            itemViewModel.loadInterestedUsers()
            this.findNavController().navigate(R.id.action_showItemFragment_to_usersInterestedFragment)
        }
    }
}
