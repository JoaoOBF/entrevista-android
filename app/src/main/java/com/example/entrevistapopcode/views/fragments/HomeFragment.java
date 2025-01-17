package com.example.entrevistapopcode.views.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.entrevistapopcode.R;
import com.example.entrevistapopcode.RecyclerItemClickListener;
import com.example.entrevistapopcode.api.entity.API;
import com.example.entrevistapopcode.api.entity.config.RetrofitConfig;
import com.example.entrevistapopcode.api.entity.entity.Person;
import com.example.entrevistapopcode.api.entity.entity.SwaResult;
import com.example.entrevistapopcode.db.view.PersonViewModel;
import com.example.entrevistapopcode.util.InternetCheck;
import com.example.entrevistapopcode.view.adapters.Adapter;
import com.example.entrevistapopcode.views.DetalhesActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements Callback<SwaResult> {

    Call<SwaResult> call;
    API service;
    String page;
    RecyclerView mRecyclerView;
    ArrayList<Person> itens;
    private Adapter mAdapter;
    private PersonViewModel viewModel;
    private SearchView searchView;
    private ProgressBar progressBar;
    private  Observer<List<Person>> obsever;
    private String searchPerson;
    private boolean search = false;



    public  HomeFragment newInstance() {
        return new HomeFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment, container, false);


        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv);

        searchView=(SearchView) view.findViewById(R.id.search);
        progressBar=(ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        searchView.setQueryHint("Pesquise aqui");


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchPerson = query;
                searchView.clearFocus();
                searchView.setQueryHint("Pesquise aqui");
                search = false;
                viewModel.getAllPersonById("%"+searchPerson+"%").observe((AppCompatActivity)getContext(), obsever);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                searchPerson = searchView.getQuery().toString();
                if(!b && searchPerson.length()>0){

                    searchView.clearFocus();
                    searchView.setQueryHint("Pesquise aqui");
                    search = false;
                    viewModel.getAllPersonById("%"+searchPerson+"%").observe((AppCompatActivity)getContext(), obsever);
                }
            }
        });
        viewModel = ViewModelProviders.of(this).get(PersonViewModel.class);
        new InternetCheck(internet -> {
            if(!internet){
                doOffline();
            }


        });


        setupRecycler();
        return  view;
    }


    @Override
    public void onResponse(Call<SwaResult> call, Response<SwaResult> response) {
        try {
            progressBar.setVisibility(View.GONE);
            SwaResult cep = response.body();
            for(Person p : cep.results){

                mAdapter.updateList(p);
                viewModel.insert(p);


            }
            if (cep.getNext()!=null){


                page = cep.getNext()
                        .substring(cep.getNext().indexOf('=') + 1);
                Log.e("Resultado   ", "resultado da busca" + cep.getNext());
                Call<SwaResult> callPage = service.getPersonPage(page);
                callPage.enqueue(this);

            }


        }catch (Exception e){

        }

    }

    @Override
    public void onFailure(Call<SwaResult> call, Throwable t) {

    }
    private void setupRecycler() {

        // Configurando o gerenciador de layout para ser uma lista.
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        mRecyclerView.setLayoutManager(layoutManager);

        // Adiciona o adapter que irá anexar os objetos à lista.
        // Está sendo criado com lista vazia, pois será preenchida posteriormente.
        mAdapter = new Adapter(new ArrayList<>(0));
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setViewModel(viewModel);
        mAdapter.setActivity(getActivity());
        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), mRecyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        int itemPosition = mRecyclerView.getChildLayoutPosition(view);
                        Person p = mAdapter.getList().get(itemPosition);
                        Intent i = new Intent(getContext(),DetalhesActivity.class);
                        i.putExtra("person", (Serializable) p);
                        startActivity(i);
                    }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );

        service = new RetrofitConfig(false).getSwa();
        call = service.getPerson();
        call.enqueue(this);

    }
    private void  doOffline(){
        progressBar.setVisibility(View.GONE);
        viewModel.getAllWords().observe(getActivity(), new Observer<List<Person>>() {
            @Override
            public void onChanged(@Nullable final List<Person> words) {

                mAdapter = new Adapter(words);
                mAdapter.setViewModel(viewModel);
                mAdapter.setActivity(getActivity());
                mRecyclerView.setAdapter(mAdapter);

            }
        });
    }
    private void alert(List<Person> list) {
        if(!search){
            search = true;
            Log.e("alerta","entrou no alerta");
            AlertDialog alerta=null;
            //LayoutInflater é utilizado para inflar nosso layout em uma view.
            //-pegamos nossa instancia da classe
            LayoutInflater li = getLayoutInflater();

            //inflamos o layout alerta.xml na view
            View itemView = li.inflate(R.layout.custom_dialog, null);
            //definimos para o botão do layout um clickListener
            RecyclerView rv = (RecyclerView) itemView.findViewById(R.id.rv);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            rv.setLayoutManager(layoutManager);
            Adapter adapter = new Adapter(list);
            adapter.setViewModel(viewModel);
            adapter.setActivity(getActivity());
            rv.setAdapter(adapter);


            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Encontramos: "+list.size()+" resultados");
            //define a mensagem
            builder.setMessage("may the force be with you");
            builder.setView(itemView);
            builder.setNeutralButton("Fechar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            rv.addOnItemTouchListener(
                    new RecyclerItemClickListener(getContext(), mRecyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                        @Override public void onItemClick(View view, int position) {
                            int itemPosition = mRecyclerView.getChildLayoutPosition(view);
                            Person p = mAdapter.getList().get(itemPosition);
                            Intent i = new Intent(getContext(),DetalhesActivity.class);
                            i.putExtra("person", (Serializable) p);
                            startActivity(i);
                        }

                        @Override public void onLongItemClick(View view, int position) {
                            // do whatever
                        }
                    })
            );
            alerta = builder.create();
            alerta.show();
        }





    }

}
