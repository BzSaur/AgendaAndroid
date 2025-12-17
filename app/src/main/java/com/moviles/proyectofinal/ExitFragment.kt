package com.moviles.proyectofinal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class ExitFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSalir = view.findViewById<Button>(R.id.btnSalir)
        btnSalir.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("¿Deseas salir de la app?")
                .setMessage("Se cerrará completamente la aplicación.")
                .setPositiveButton("Salir") { _, _ ->
                    requireActivity().finishAffinity()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}
