public class Main {
    public static void main(String[] args) {
        Student student = new Student("Ada", "SLAM");
        System.out.println(student.describe());
    }
}

record Student(String name, String specialty) {
    String describe() {
        return name + " étudie en spécialité " + specialty + ".";
    }
}

