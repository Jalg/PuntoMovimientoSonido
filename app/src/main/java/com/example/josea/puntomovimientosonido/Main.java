package com.example.josea.puntomovimientosonido;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Queue;
import java.util.Scanner;

public class Main extends AppCompatActivity implements SensorEventListener {
    private SensorManager sm;          //Controlador del sensor
    private Sensor accel;	           //Estructura para el aceler�metro
    private int estado;                //Controla el estado del programa (registrando o detectando movimiento)
    private MediaPlayer sonido = null; //Reproductor de sonidos
    //Estructuras de datos para reconocer gestos
    private ArrayList<ArrayList<Float>> datos = new ArrayList<ArrayList<Float>>();  //Almacena el registro del gesto (memorizaci�n del gesto)
    private ArrayList<Float> guardados = new ArrayList<Float>();  //Almacena el registro del gesto (memorizaci�n del gesto)
    private Queue<ArrayList<Float>> buffer    = new ArrayDeque<ArrayList<Float>>(); //Cola que almacena los �ltimos movimientos hechos por el usuario (detecci�n)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Inicializamos el sensor
        sm     = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel  = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Cargamos el sonido que se reproducir�
        sonido = MediaPlayer.create(this, R.raw.disparo);

        leerFichero();

    }
    protected void onPause(){
        sm.unregisterListener(this);
        super.onPause();
    }
    protected void onStop()  { super.onStop();   }
    protected void onResume(){ super.onResume(); }

    /**************************************
     * Eventos propios del sensor         *
     **************************************/
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    //Cuando se detecten nuevos datos en el sensor
    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            //Si se ha pulsado memorizar movimiento...
            if(estado!=0){
                detectarMovimiento(event);
            }
        }
    }

    //Activa o desactiva el reconocimiento de movimientos (toggleButton iniciar/detener detector)
    public void reconocerMovimiento(View v){
        //Iniciar reconocimiento
        if(((CompoundButton) v).isChecked() && datos.size()>0){
            //Cambiamos estado y comenzamos a escuchar el aceler�metro
            estado = 1;
            buffer.clear();
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }else{ //detener reconocimiento
            //Cambiamos estado y cerramos la escucha del aceler�metro
            estado = 0;
            sm.unregisterListener(this);
        }
    }

    //Almacena un movimiento cuando se est� reconocimendo gestos
    //y determina si el gesto realizado es el mismo que el memorizado
    private void detectarMovimiento(SensorEvent event){
        //Array para amacenar las tres coordenadas
        ArrayList<Float> coords       = new ArrayList<Float>(3);

        //Redondeamos a 3 decimales
        coords.add((float) (Math.round(event.values[0] * 1000.0) / 1000.0));
        coords.add((float) (Math.round(event.values[1] * 1000.0) / 1000.0));
        coords.add((float) (Math.round(event.values[2] * 1000.0) / 1000.0));

        //Llenamos el buffer y lo mantenemos al mismo tama�o que el gesto memorizado
        if(buffer.size()==datos.size()){
            buffer.poll();
            buffer.add(coords);
            //Reconocemos el gesto seg�n el m�todo que queramos
            reconocimientoDifCuadrados();
        }else
            buffer.add(coords);
    }

    //Reconoce un gesto haciendo diferencia de cuadrados por coordenadas
    private void reconocimientoDifCuadrados(){
        float sumaX, sumaY, sumaZ;
        float umbral1=700, umbral2=8000;
        int i;
        Iterator<ArrayList<Float>> coordsIt;
        ArrayList<Float> coordsBuffer = new ArrayList<Float>(3);

        coordsIt = buffer.iterator();
        i = 0;
        sumaX = sumaY = sumaZ = 0;
        //Recorremos el buffer y calculamos las diferencias por coordenadas
        while(coordsIt.hasNext()){
            coordsBuffer = coordsIt.next();
            sumaX += Math.pow((datos.get(i).get(0)-coordsBuffer.get(0)), 2);
            sumaY += Math.pow((datos.get(i).get(1)-coordsBuffer.get(1)), 2);
            sumaZ += Math.pow((datos.get(i).get(2)-coordsBuffer.get(2)), 2);
            i++;
        }

        //Si no se supera el umbral, se acepta el gesto realizado. Reproducimos sonido y vaciamos el buffer
        if(sumaX<umbral1 && sumaY<umbral1 && sumaZ<umbral1 &&(sumaX+sumaY+sumaZ)<umbral2){
            System.out.println("Diferencia [X, Y, Z]: [" + sumaX + ", "+ sumaY + ", "+ sumaZ + "]");
            sonido.start();
            buffer.clear();
        }
    }

    public void leerFichero() {
        String texto = "";
        Scanner in = null;
        try {
            InputStream mov = getResources().openRawResource(R.raw.movimiento);
            BufferedReader buffer_mov = new BufferedReader(new InputStreamReader(mov));
            in = new Scanner(buffer_mov);
            in.useLocale(Locale.ENGLISH);

            while (in.hasNextFloat()) {
                // lee un double
                float d = in.nextFloat();
                guardados.add(d);
            }
        } catch (Exception e) {}

        in.close();

        //Array para amacenar las tres coordenadas
        ArrayList<Float> coords = new ArrayList<Float>();
        for(int i=0; i<guardados.size(); i+=3) {
            crearMov(i);
        }
    }

    public void crearMov(int i){
        ArrayList<Float> coords = new ArrayList<Float>(3);

        coords.add(guardados.get(i));
        coords.add(guardados.get(i + 1));
        coords.add(guardados.get(i+2));

        datos.add(coords);
    }

    public void presionarCierre(){
        System.exit(0);
    }
}
