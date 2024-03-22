package com.example.onactivityresulttest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.onactivityresulttest.databinding.FragmentFirstBinding
import java.io.File


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private val binding get() = _binding!!

    private val filePicker = FilePicker(this)

//    private val pickImage =
//        registerForActivityResult(ActivityResultContracts.GetContent()) { contentUri ->
//            with(binding.imageView) {
//                dispose()
//                load(contentUri) {
//                    listener(
//                        onError = { request, errorResult ->
//                            Toast.makeText(
//                                request.context,
//                                errorResult.throwable.message,
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    )
//                }
//            }
//        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
           filePicker.launch(object : FilePickerResultCallback{
               override fun result(file: File) {
                   binding.fileName.text = file.name
               }

           })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

