package com.example;

import com.example.model.Department;
import com.example.model.Employee;
import jakarta.persistence.Query;
import org.hibernate.Session;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world");

        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Department department = new Department("java");
        session.persist(department);
        session.persist(new Employee("Jakab Gipsz",department));
        session.persist(new Employee("Captain Nemo",department));
        session.getTransaction().commit();
        Query q = session.createQuery("From Employee ", Employee.class);
        List<Employee> resultList = q.getResultList();
        System.out.println("num of employees:" + resultList.size());
        for (Employee next : resultList) {
            System.out.println("next employee: " + next);
        }
    }

}
