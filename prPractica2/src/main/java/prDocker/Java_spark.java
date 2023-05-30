package prDocker;

import static spark.Spark.get;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import prMarkovChains.DetectorAnomalias;
import redis.clients.jedis.Jedis;

import com.google.gson.Gson;

public class Java_spark {
	// Intenta conectarse con REDIS_HOST y si no puede lo hará con localhost
	static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
	private static int tamVentana = 4;	// Igual que en el proyecto de Cadenas de Markov
	private static Gson gson = new Gson();
	
	public static String escribirDatos(Medicion dato) {
		try {
			Jedis jedis = new Jedis(REDIS_HOST);	// Conexión
			DateFormat df = new SimpleDateFormat("M dd yyyy HH:mm:ss");
			jedis.rpush("queue#fechas", df.format(dato.fecha));
			jedis.rpush("queue#datos", (dato.valor).toString());
			
			String s = "";
			// Obtener la cantidad de datos que hay:
			long tam = jedis.llen("queue#fechas");
			// Se puede estudiar si hay alguna anomalia?
			if(tam >= tamVentana) {	// Se puede. Hay al menos 4 valores en la BD
				String val = "";
				/* Se guardarán en una lista las últimas 4 mediciones
				   que se han insertado en la base de datos */
				List<Integer> mediciones = new ArrayList<>();
				for(long i = tam-tamVentana; i < tam; i++) {
					val = jedis.lindex("queue#datos", i);
					mediciones.add(Integer.parseInt(val));
				}
				// Se estudia si la última ventana introducida origina una anomalía
				s = DetectorAnomalias.detectarAnomaliaPr2(mediciones);
			}
			// Muestra el valor (medida + hora) introducido en la base de datos
			String res = "Se ha introducido: " + gson.toJson(dato); 
			jedis.close();
			if(tam < tamVentana) {	// Hacen falta más medidas para poder tectar anomalías
				long faltan = tamVentana-tam;
				return gson.toJson(res + ". Hacen falta al menos " + faltan + 
						" mediciones más para detectar si se ha producido una anomalia"); 
			}else {
				// Muestra el valor introducido y si se ha originado una anomalía
				return gson.toJson(res + s);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return gson.toJson("NO se ha podido escribir el dato " + gson.toJson(dato));
		}
	}
	
	public synchronized static String obtenerDatos() {
		String s;
		try {
			Jedis jedis = new Jedis(REDIS_HOST);	// Conexión
			String fecha = "", dato = "";
			List<Integer> mediciones = new ArrayList<>();
			List<Medicion> fecha_valor = new ArrayList<>();
			DateFormat df = new SimpleDateFormat("M dd yyyy HH:mm:ss");
			// Obtiene la cantidad de mediciones que hay en la BD:
			long tam = jedis.llen("queue#fechas");
			// Comprueba que haya un numero mayor o igual de datos que la ventana
			if(tam < tamVentana) {	// No se puede detectar anomalia
				// Obtiene las mediciones de la BD:
				for(int i = 0; i < tam; i++) {
					fecha = jedis.lindex("queue#fechas", i);
					dato = jedis.lindex("queue#datos", i);
					
					Medicion med = new Medicion();
					med.fecha = df.parse(fecha);				
					med.valor = Integer.parseInt(dato);
					// Para mostrar las mediciones (valor + hora) como lista:
					fecha_valor.add(med);
				}
				long faltan = tamVentana-tam;
				Respuesta res = new Respuesta();
				res.mediciones = (int)tam;
				res.datos = gson.toJson(fecha_valor);
				// Faltan datos para poder detectar anomalias
				res.anomalia = "Hay " + tam + " mediciones, deben introducirse al menos "
						+ faltan + " datos más para poder detectar posibles anomalías";
				s = gson.toJson(res);
			}else {	// Se puede detectar anomalia
				for(int i = 0; i < tam; i++) {
					fecha = jedis.lindex("queue#fechas", i);
					dato = jedis.lindex("queue#datos", i);
					
					Medicion med = new Medicion();
					med.fecha = df.parse(fecha);				
					med.valor = Integer.parseInt(dato);
					/* Mete en una lista de mediciones todos los valores para estudiar
					   posteriormente si alguna de las ventanas originadas provoca una
					   anomalía: DetectorAnomalias.detectarAnomaliaPr2(mediciones)*/
					mediciones.add(med.valor);
					// Para mostrar las mediciones (valor + hora) como lista:
					fecha_valor.add(med);				
				}
				// Formato JSON:
				Respuesta res = new Respuesta();
				res.mediciones = mediciones.size();
				res.datos = gson.toJson(fecha_valor);
				res.anomalia = DetectorAnomalias.detectarAnomaliaPr2(mediciones);
				s = gson.toJson(res);
			}	
			jedis.close();
			return s;
		} catch (Exception ex) {	
			return gson.toJson(ex);
		}
	}

	public static void main(String[] args) {
		try {
			System.out.println("Conectando con " + REDIS_HOST);
			Jedis jedis = new Jedis(REDIS_HOST);
			jedis.flushAll();	// Borra el contenido previo		
			jedis.close();
		} catch (Exception ex) { }
		leerDatos();
		mostrarDatos();
	}

	private static void leerDatos() {
		// Detectar anomalias en los ultimos 4 introducidos
		get("/nuevo/:dato", (req, res)->{
			Medicion nueva = new Medicion();
			nueva.fecha = new Date();	// Instante actual
			nueva.valor = Integer.parseInt(req.params(":dato"));
			return escribirDatos(nueva);
		});
	}
	
	private static void mostrarDatos() {
		get("/listar", (req, res)->{
			return obtenerDatos();
		});
	}
	
}

