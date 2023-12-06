package com.example;

import com.example.minimodel.Location;
import com.example.model.Department;
import com.example.model.Employee;
import jakarta.persistence.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world");

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        Location location = new Location();
        location.setTitle("Darkness");
        session.persist(location);
        transaction.commit();
        Query q = session.createQuery("From Location ", Location.class);
        List<Location> resultList = q.getResultList();
        System.out.println("num of locations:" + resultList.size());
        for (Location next : resultList) {
            System.out.println("next location: (" + next.id + ") - " + next.title);
        }
    }

}
