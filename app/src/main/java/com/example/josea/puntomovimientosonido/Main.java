package com.example.josea.puntomovimientosonido;

/*
    Copyright (C) 2016  José Miguel Navarro Moreno and José Antonio Larrubia García
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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

/**
 *
 * @Authors  José Miguel Navarro Moreno and José Antonio Larrubia García
 *
 * Clase principal y única de la aplicación.
 * Al pulsar el botón principal de la misma se comienza a detectar el movimiento,
 * si se detecta el movimiento metido en el fichero dentro de la carpeta raw se escuchará un sonido,
 * que también se encuentra dentro de la carpeta raw.
 * Para realizar la aplicación nos hemos basado sobre todo en los siguientes enlaces.
 * 1.- Código usado como base para generar el fichero para detectar el movimiento y el uso del acelerómetro:
 * https://github.com/franciscovelez/DetectorDeGestos-Android
 * 2.- Para la lectura de ficheros:
 * https://docs.oracle.com/javase/7/docs/api/java/util/Scanner.html
 * 3.- Para que se escuche el sonido:
 * http://www.javaya.com.ar/androidya/detalleconcepto.php?codigo=152&inicio=20
 */
public class Main extends AppCompatActivity implements SensorEventListener {

    /**
     *  Controlador del sensor
     */
    private SensorManager sm;         

    /**
     *  Estructura para el acelerómetro
     */
    private Sensor accel;

    /**
     *  Controla el estado del programa (detectando o no el movimiento)
     */
    private int estado;               

    /**
     *  Reproductor de sonidos
     */
    private MediaPlayer sonido = null;

    /**
     *  Estructura de datos para reconocer gestos final.
     */
    private ArrayList<ArrayList<Float>> datos = new ArrayList<ArrayList<Float>>();

    /**
     *  Estructuras de datos para reconocer gestos que usaremos de forma auxiliar al leer el fichero.
     */
    private ArrayList<Float> guardados = new ArrayList<Float>(); 

    /**
     *  Cola que almacena los últimos movimientos hechos por el usuario para detectar si es similar al nuestro.
     */
    private Queue<ArrayList<Float>> buffer    = new ArrayDeque<ArrayList<Float>>();

    /**
     * Función para iniciar la aplicación.
     * @param savedInstanceState Instancia de la aplicación, usada y pasada por android
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Inicializamos el sensor y el controlador del sensor
        sm     = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel  = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
		//Cargamos el sonido que se reproducirá.
        sonido = MediaPlayer.create(this, R.raw.disparo);
	
		//Leemos el fichero.
        leerFichero();

    }

    /**
     *  Cuando la aplicación se pare se llamará a ésta función.
     */
    protected void onPause(){
        sm.unregisterListener(this);
        super.onPause();
    }

    /**
     *  Cuando se pare la aplicación se llamará a ésta función.
     */
    protected void onStop()  { super.onStop();   }

    /**
     *  Cuando vuelva de la pausa se llama a esta función
     */
    protected void onResume(){ super.onResume(); }

    /**
     * Método para destruir la aplicación cuando se cierra.
     */
    protected void onDestroy() { super.onDestroy();}

    /**
     *  Evento propio del sensor para detectar cambios de precisión en nuestro caso no la usamos pero hace falta que esté declarada.
     * @param sensor sensor activo.
     * @param accuracy precisión del cambio del sensor.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    /**
     *  Evento del sensor, se llamará cuando se detecten nuevos datos(cambios) en el sensor.
     * @param event evento que usaremos para leer los valores del acelerómetro en la detección del movimiento
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            //Si se ha pulsado el botón para detectar el movimientos se empieza a detectar.
            if(estado!=0){
                detectarMovimiento(event);
            }
        }
    }

    /**
     *  Botón que activa o desactiva el reconocimiento de movimientos (toggleButton iniciar/detener detector)
     * @param v  Componente necesario para comprobar si está o no pulsado el botón
     */
    public void reconocerMovimiento(View v){
        //Iniciar reconocimiento
        if(((CompoundButton) v).isChecked() && datos.size()>0){
            //Cambiamos estado y comenzamos a escuchar el acelerómetro
            estado = 1;
            buffer.clear();
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        }else{ 
			//detener reconocimiento
            //Cambiamos estado y cerramos la escucha del acelerómetro
            estado = 0;
            sm.unregisterListener(this);
        }
    }

    /**
     * Almacena un movimiento cuando se estén reconocimendo gestos y determina si el gesto realizado es el mismo que el memorizado
     * @param event Pasado por onSensorChanged, contiene los valores del acelerómetro.
     */
    private void detectarMovimiento(SensorEvent event){
        //Array para amacenar las tres coordenadas
        ArrayList<Float> coords       = new ArrayList<Float>(3);

        //Redondeamos a 3 decimales
        coords.add((float) (Math.round(event.values[0] * 1000.0) / 1000.0));
        coords.add((float) (Math.round(event.values[1] * 1000.0) / 1000.0));
        coords.add((float) (Math.round(event.values[2] * 1000.0) / 1000.0));

        //Llenamos el buffer y lo mantenemos al mismo tamaño que el gesto memorizado
        if(buffer.size()==datos.size()){
            buffer.poll();
            buffer.add(coords);
            //Reconocemos el gesto.
            reconocimientoDifCuadrados();
        }else
            buffer.add(coords);
    }

    /**
     *  Reconoce un gesto haciendo diferencia de cuadrados por coordenadas
     */
    private void reconocimientoDifCuadrados(){
        //Sumas para hacer la diferencia de cuadrados.
		float sumaX, sumaY, sumaZ;
		
		// Los umbrales siguientes son lo que determina como de igual debe ser el gesto a realizar para que coincida con el nuestro.
        float umbral1=700, umbral2=8000;
        
		//Variables iteradora y contadora.
		int i;
        Iterator<ArrayList<Float>> coordsIt;
		
		// Arraylist donde almacenaremos auxiliarmente las coordenadas del buffer que contengan la iteradora en ese momento.
        ArrayList<Float> coordsBuffer = new ArrayList<Float>(3);

		// Inicilizamos sumatorias, contadora, iteradora y el buffer auxiliar.
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

    /**
     *  Función que se usa para leer el fichero con el movimiento almacenado.
     */
    public void leerFichero() {
		//Iniciamos el scanner para leer el fichero.
        Scanner in = null;
		
        try {
			//Iniciamos el stream de lectura y el buffer posteriormente con dicho stream.
            InputStream mov = getResources().openRawResource(R.raw.movimiento);
            BufferedReader buffer_mov = new BufferedReader(new InputStreamReader(mov));
			
			// Pasamos al scanner el buffer de lectura y asignamos el lugar del mismo.
            in = new Scanner(buffer_mov);
            in.useLocale(Locale.ENGLISH);

			// Mientras haya fichero se van guardando en el arraylist auxiliar los datos
            while (in.hasNextFloat()) {
                // lee un double
                float d = in.nextFloat();
                guardados.add(d);
            }
        } catch (Exception e) {}
		
		// Cuando acabe se cierra el scanner
        in.close();

		// Y por último creamos el movimiento final.
        for(int i=0; i<guardados.size(); i+=3) {
            crearMov(i);
        }
    }

    /**
     * Función para crear el movimiento final a partir del movimiento dado sin tener forma de coordenadas.
     * @param i Posición del arraylist auxiliar para coger los siguientes 3 datos para hacer las coordenadas.
     */
    public void crearMov(int i){
		
		//Array para amacenar las tres coordenadas
        ArrayList<Float> coords = new ArrayList<Float>(3);

        coords.add(guardados.get(i));
        coords.add(guardados.get(i + 1));
        coords.add(guardados.get(i+2));

        datos.add(coords);
    }
}
