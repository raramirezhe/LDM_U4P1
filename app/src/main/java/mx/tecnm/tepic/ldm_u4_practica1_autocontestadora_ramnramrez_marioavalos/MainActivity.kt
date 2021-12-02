package mx.tecnm.tepic.ldm_u4_practica1_autocontestadora_ramnramrez_marioavalos

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.icu.util.Calendar.DATE
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.CallLog
import android.provider.CallLog.Calls.DATE
import android.provider.Telephony.TextBasedSmsColumns.DATE
import android.provider.VoicemailContract.Voicemails.DATE
import android.telephony.SmsManager
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.common.net.HttpHeaders.DATE
import com.google.firebase.firestore.FirebaseFirestore
import io.grpc.CallOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.sql.Types.DATE
import java.text.SimpleDateFormat
import java.util.Calendar.DATE
import javax.xml.datatype.DatatypeConstants.DATE

class MainActivity : AppCompatActivity() {
    val baseRemota = FirebaseFirestore.getInstance()
    var listaTelefonos = ArrayList<String>()
    var listaMensajesEnviados = ArrayList<String>()
    val siLecturaLlamadas = 1
    val siEnviarMensaje = 2
    var mensajeAgradable = "Por el momento no puedo responder, en cuanto pueda te regreso la llamada"
    var mensajeDesagradable = "Deja de llamarme, no atendere tu llamada"


    val timer = object : CountDownTimer(20000, 5000) {
        override fun onTick(millisUntilFinished: Long) {
            cargarListaLlamadas()
        }

        override fun onFinish() {
            enviarSMS()
            start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if(ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.READ_CALL_LOG),siLecturaLlamadas)
        }

        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.SEND_SMS),siEnviarMensaje)
        }

        btnMensajes.setOnClickListener {
            actualizarMensajes()
        }

        btnAgregarContacto.setOnClickListener {
            insertarContacto()
        }

        tbActivar.setOnClickListener {
            if (tbActivar.text.toString() == "ON") {
                alerta("ACTIVADO")
                timer.start()

            } else{
                alerta("DESACTIVADO")
                timer.cancel()
            }
        }





    }


    private fun insertarContacto() {
        var tipo = ""
        if(rbtListaB.isChecked){
            tipo = rbtListaB.text.toString()
        }else{
            tipo = rbtListaN.text.toString()
        }

        var datosInsertar = hashMapOf(
            "nombre" to txtNombre.text.toString(),
            "telefono" to txtTelefono.text.toString(),
            "tipo" to tipo
        )

        baseRemota.collection("contactos")
            .add(datosInsertar)
            .addOnSuccessListener {
                mensaje("SE INSERTO CORRECTAMENTE")

                txtNombre.setText("")
                txtTelefono.setText("")
            }
            .addOnFailureListener{
                mensaje("SYNTAX ERROR - NO SE PUDO INSERTAR")
            }

    }

    private fun enviarSMS(){
        var tipo = ""
        if(listaTelefonos.isNotEmpty()){
            var telefono = ""
            listaTelefonos.forEach {
                baseRemota.collection("contactos").addSnapshotListener { querySnapshot, error ->
                    if(error!=null){
                        mensaje(error.message!!)
                        return@addSnapshotListener
                    }
                    for(document in querySnapshot!!){
                        tipo = "${document.getString("tipo")}"
                        telefono = document.getString("telefono").toString()

                        if(listaMensajesEnviados.contains(telefono)){

                        }else{
                            if(tipo.equals("AGRADABLES")){
                                if(it.equals(document.getString("telefono"))){
                                    SmsManager.getDefault().sendTextMessage(telefono,null,mensajeAgradable,null,null)
                                    listaMensajesEnviados.add(telefono)
                                }
                            }else{
                                if(it.equals(document.getString("telefono"))){
                                    SmsManager.getDefault().sendTextMessage(telefono,null,mensajeDesagradable,null,null)
                                    listaMensajesEnviados.add(telefono)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun actualizarMensajes(){
        mensajeAgradable = txtMensajeA.text.toString()
        mensajeDesagradable = txtMensajeD.text.toString()
        mensaje("MENSAJES ACTUALIZADOS")
        txtMensajeA.setText(" ")
        txtMensajeD.setText("")
    }

    private fun mensaje(s: String) {
        AlertDialog.Builder(this).setTitle("ATENCIÓN")
            .setMessage(s)
            .setPositiveButton("OK"){d,i->}
            .show()
    }

    private fun alerta(s:String){
        Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    }

    @SuppressLint("Range")
    private fun cargarListaLlamadas(){
        var llamadas = ArrayList<String>()
        val seleccion = CallLog.Calls.TYPE+"="+CallLog.Calls.MISSED_TYPE
        var cursor = contentResolver.query(
            Uri.parse("content://call_log/calls"),null,seleccion,null,null
        )
        listaTelefonos.clear()
        var registro = ""
        while (cursor!!.moveToNext()){
            var nombre = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME))
            var telefono = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
                telefono = telefono.replace(" ".toRegex(),"")
            var fecha = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE))



            registro = "NOMBRE: ${nombre}\nNumero:${telefono}\nFecha: ${fecha}"
            llamadas.add(registro)
            listaTelefonos.add(telefono)
        }
        listaLlamadas.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,llamadas)
        cursor.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == siLecturaLlamadas){cargarListaLlamadas()}
    }

}