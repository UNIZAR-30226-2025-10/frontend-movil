package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.example.myapplication.R

class Pendiente : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.solicitud_pendiente)

        // Botones
        val eliminar = findViewById<Button>(R.id.btn_delete)
        val logout = findViewById<Button>(R.id.btn_close)

        eliminar.setOnClickListener {
            // Ir a pantalla de login normal
            showDeleteAccountDialog()
            //startActivity(Intent(this, Inicio::class.java))
        }

        logout.setOnClickListener {
            // Ir a pantalla de  elegir tipo de registro
            startActivity(Intent(this, Logout::class.java))
        }

    }

    private fun showDeleteAccountDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_account)

        val window: Window? = dialog.window
        window?.setLayout((Resources.getSystem().displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.setCancelable(true)

        val editPassword = dialog.findViewById<EditText>(R.id.editPassword)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnTogglePassword = dialog.findViewById<ImageButton>(R.id.btnTogglePassword)

        val font = ResourcesCompat.getFont(this, R.font.poppins_regular)
        val typefaceSpan = TypefaceSpan(font!!)
        var passwordVisible = false

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible

            if (passwordVisible) {
                // Mostrar la contraseña
                editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_on)
            } else {
                // Ocultar la contraseña
                editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            }

            // Mantener el cursor al final
            editPassword.setSelection(editPassword.text.length)
            // Aplicar fuente personalizada si deseas
            val text = editPassword.text.toString()
            val spannable = SpannableString(text)
            spannable.setSpan(typefaceSpan, 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            editPassword.setText(spannable)
        }

        btnConfirm.setOnClickListener {
            val password = editPassword.text.toString().trim()
            if (password.isNotEmpty()) {
                val intent = Intent(this, DeleteAccount::class.java)
                intent.putExtra("password", password)
                startActivity(intent)
                dialog.dismiss()
            } else {
                showToast("Introduce tu contraseña")
            }
        }

        dialog.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
