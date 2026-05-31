package com.ycon.validadorinventario.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ycon.validadorinventario.databinding.BottomSheetSaldoSkuBinding
import com.ycon.validadorinventario.ui.adapter.SaldoSkuAdapter

class SaldoSkuBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSaldoSkuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventarioViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[InventarioViewModel::class.java]
    }
    private val adapter = SaldoSkuAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSaldoSkuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerSaldos.adapter = adapter

        viewModel.saldosPorSku.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
            binding.txtSaldoVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SaldoSkuBottomSheet"
    }
}
