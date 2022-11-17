// -*- coding: utf-8 -*-

import java.util.Random ;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TriRapide {
    static final int taille = 1_000_000 ;                   // Longueur du tableau à trier
    static final int [] tableau = new int[taille] ;         // Le tableau d'entiers à trier
    static int [] tableau2 = new int[taille] ;
    static final int borne = 10 * taille ;                  // Valeur maximale dans le tableau
    //Défintion de l'executeur et du service.
    static ExecutorService executeur = Executors.newFixedThreadPool(4);
    static CompletionService<Void> service = new ExecutorCompletionService<Void>(executeur);
    static AtomicInteger nbTaches = new AtomicInteger(0); //Nombre des taches nécessaires pour le tri rapide.

    private static void echangerElements(int[] t, int m, int n) {
        int temp = t[m] ;
        t[m] = t[n] ;
        t[n] = temp ;
    }

    private static int partitionner(int[] t, int début, int fin) {
        int v = t[fin] ;                               // Choix (arbitraire) du pivot : t[fin]
        int place = début ;                            // Place du pivot, à droite des éléments déplacés
        for (int i = début ; i<fin ; i++) {            // Parcours du *reste* du tableau
            if (t[i] < v) {                            // Cette valeur t[i] doit être à droite du pivot
                echangerElements(t, i, place) ;        // On le place à sa place
                place++ ;                              // On met à jour la place du pivot
            }
        }
        echangerElements(t, place, fin) ;              // Placement définitif du pivot
        return place ;
    }

    private static void trierRapidement(int[] t, int début, int fin) {
        if (début < fin) {                             // S'il y a un seul élément, il n'y a rien à faire!
            int p = partitionner(t, début, fin) ;      
            trierRapidement(t, début, p-1) ;
            trierRapidement(t, p+1, fin) ;
        }
    }
    //---------------- Début de Tri Parallèle ---------------------------------------------
    private static void trierRapidementAccéléré(int[] t, int début, int fin) { // Le tri Parallèle
        if (début >= fin) return;
        int p = partitionner(t, début, fin) ;
        if ((fin - début) > taille/100) { // Les sous tableaux sont traités en parallèle si la taille des sous tableaux est supérieur à "taille / 100"
            service.submit(() -> trierRapidementAccéléré(t, début, p-1), null); //Partitionner la 1 ère partition de façon parallèle
            service.submit(() -> trierRapidementAccéléré(t, p+1, fin), null); //Partitionner la 2 ème partition de façon parallèle
            nbTaches.addAndGet(2); //Après chaque partitionnement on ajoute 2 Taches ( 1 tache pour une partition )
        }
        else { // On garde l'approche séquentielle si la taille des sous tableaux est inférieur à "taille / 100"
            trierRapidement(t, début, p-1);//Partitionner la 1 ère partition de façon séquentielle
            trierRapidement(t, p+1, fin);//Partitionner la 2 ème partition de façon séquentielle
        }
    }
    private static boolean testIdentique (int[] t1, int[] t2){ //Vérification si les deux tableaux triés sont identiques.
        for (int i=0 ; i<taille ; i++) {
            if(t1[i] != t2[i])
                return false;
        }
        return true;
    }
    //---------------- Fin de Tri Parallèle ---------------------------------------------
    private static void afficher(int[] t, int début, int fin) {
        for (int i = début ; i <= début+3 ; i++) {
            System.out.print(" " + t[i]) ;
        }
        System.out.print("...") ;
        for (int i = fin-3 ; i <= fin ; i++) {
            System.out.print(" " + t[i]) ;
        }
        System.out.println() ;
    }

    public static void main(String[] args) {
        Random aléa = new Random() ;
        for (int i=0 ; i<taille ; i++) {                          // Remplissage aléatoire du tableau
            tableau[i] = aléa.nextInt(2*borne) - borne ;            
        }
        System.arraycopy(tableau, 0, tableau2, 0, tableau.length);
        System.out.print("Tableau initial : ") ;
        afficher(tableau, 0, taille -1) ;                         // Affiche le tableau à trier

        System.out.println("Démarrage du tri rapide séquentielle.") ;
        long débutDuTri = System.nanoTime();

        trierRapidement(tableau, 0, taille-1) ;                   // Tri du tableau

        long finDuTri = System.nanoTime();
        long duréeDuTri = (finDuTri - débutDuTri) / 1_000_000 ;
        System.out.print("Tableau trié : ") ; 
        afficher(tableau, 0, taille -1) ;                         // Affiche le tableau obtenu
        System.out.println("Version séquentielle obtenu en " + duréeDuTri + " millisecondes.") ;


        // Tri Version parallèle
        System.out.println("Démarrage du tri rapide parallèle.") ;
        long débutDuTriParal = System.nanoTime();

        trierRapidementAccéléré(tableau2, 0, taille-1);   // Tri du tableau parallèle

        //On attend que toutes les tâches soient effectuées.
        try{
            while (nbTaches.getAndDecrement() > 0) {
                service.take();
            }
        } catch (InterruptedException e) {e.printStackTrace();}
        //Fin d'attente
        executeur.shutdown();
        service = null;
        long finDuTriParal = System.nanoTime();
        long duréeDuTriParal = (finDuTriParal - débutDuTriParal) / 1_000_000 ;
        System.out.print("Tableau trié :") ;
        afficher(tableau2, 0, taille - 1) ;                         // Affiche le tableau obtenu
        System.out.println("Version parallèle obtenu en " + duréeDuTriParal + " millisecondes.") ;
        System.out.println("Gain observé : " + (duréeDuTri / duréeDuTriParal) );

        //Vérification si les deux tableaux triés sont identiques.
        if (testIdentique(tableau, tableau2))  System.out.println("Les tris sont cohérents.");
        else System.out.println("Les tris ne sont pas cohérents.");
    }
}


/*
  $ make
  javac *.java
  $ java TriRapide
  Tableau initial :  4967518 -8221265 -951337 4043143... -4807623 -1976577 -2776352 -6800164
  Démarrage du tri rapide.
  Tableau trié :  -9999981 -9999967 -9999957 -9999910... 9999903 9999914 9999947 9999964
  obtenu en 85 millisecondes.
*/
