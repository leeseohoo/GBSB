package com.example.gbsb.account

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.gbsb.MainActivity
import com.example.gbsb.databinding.ActivityAccountBinding
import com.example.gbsb.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class AccountActivity : AppCompatActivity() {
    lateinit var binding: ActivityAccountBinding
    var auth: FirebaseAuth?= null
    lateinit var currentUser: FirebaseUser
    private lateinit var accountdb: DatabaseReference
    private lateinit var infodb: DatabaseReference
    val model: AccountViewModel by viewModels()
    lateinit var info: Info

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.infoIntroduce.movementMethod=ScrollingMovementMethod()
        auth = FirebaseAuth.getInstance()
        initBtn()
        initAccount()
    }

    private val infoValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // infodb의 내용이 변경될 때의 동작
            if (snapshot.exists()) {
                val data = snapshot.getValue(Info::class.java)
                var email = data!!.email
                var call = data!!.call
                var birth = data!!.birth
                var major = data!!.major
                var introduce = data!!.introduce
                binding.infoEmail.text = email
                binding.infoCall.text = call
                binding.infoBirth.text = birth
                binding.infoMajor.text = major
                binding.infoIntroduce.text = introduce

                info = Info(email, call, birth, major, introduce)
                model.setData(info)
                model.setuId(auth!!.currentUser!!.uid)
            } else {
                Toast.makeText(this@AccountActivity, "snapshot이 존재하지 않습니다.", Toast.LENGTH_LONG).show()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(this@AccountActivity, "DB 조회 중 에러 발생", Toast.LENGTH_LONG).show()
        }
    }

    private fun initAccount() {
        currentUser=auth!!.currentUser!!
        accountdb = Firebase.database.getReference("Accounts")
        val uid = currentUser?.uid!!
        infodb = Firebase.database.getReference("Info").child(uid)

        if(uid != null){
            infodb.addValueEventListener(infoValueEventListener)
        }

    }

    private fun initBtn() {
        binding.apply {
            registerBack.setOnClickListener {
                val i= Intent(this@AccountActivity, MainActivity::class.java)
                startActivity(i)
            }
            accountinfoBtn.setOnClickListener {
                model.setData(info)
                model.setuId(currentUser?.uid!!)
                val dialog = EditInfoFragment()
                dialog.show(supportFragmentManager,"EditInfoFragment")
            }
            accountintroduceBtn.setOnClickListener {
                model.setData(info)
                model.setuId(currentUser?.uid!!)
                val dialog = EditIntroduceFragment()
                dialog.show(supportFragmentManager,"EditIntroduceFragment")
            }
            logoutUser.setOnClickListener {
                auth!!.signOut()
                val i= Intent(this@AccountActivity, LoginActivity::class.java)
                startActivity(i)
                finish()
            }
            deleteUser.setOnClickListener {
                confirmDialog()
            }
        }
    }

    private fun confirmDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("회원탈퇴")
        dialogBuilder.setMessage("정말로 회원탈퇴하시겠습니까?")
        dialogBuilder.setPositiveButton("확인") { dialog, which ->
            infodb.removeEventListener(infoValueEventListener)
            deleteDB()
            userDelete()
        }
        dialogBuilder.setNegativeButton("취소", null)
        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun deleteDB() {
        val uid = currentUser?.uid!!
        accountdb.child(uid).removeValue()
        infodb.removeValue()
    }

    private fun userDelete() {
        currentUser.delete().addOnCompleteListener {
            task->
            if(task.isSuccessful){
                val i= Intent(this@AccountActivity, LoginActivity::class.java)
                startActivity(i)
                finish()
            }else {
                val errorMessage = task.exception?.message
                Toast.makeText(this@AccountActivity, "회원탈퇴 처리 중 에러 발생: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }

}