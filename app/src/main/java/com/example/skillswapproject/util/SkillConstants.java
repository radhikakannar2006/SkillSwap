package com.example.skillswapproject.util;

import java.util.Arrays;
import java.util.List;

public class SkillConstants {

    public static final String[] PREDEFINED_SKILLS = {
            "Android Development",
            "Animation & 3D Modeling",
            "Chess",
            "Cloud Computing (AWS/GCP)",
            "Content Writing",
            "Cooking & Baking",
            "Cyber Security",
            "Data Science",
            "Digital Marketing",
            "English Communication",
            "Financial Accounting",
            "Flutter Development",
            "French",
            "German",
            "Graphic Design",
            "Guitar",
            "Java Programming",
            "Japanese",
            "Machine Learning",
            "Photography",
            "Piano",
            "Project Management",
            "Public Speaking",
            "Python Programming",
            "SEO Optimization",
            "Spanish",
            "UI/UX Design",
            "Video Editing",
            "Web Development",
            "Yoga & Fitness"
    };

    public static final String[] AVAILABILITY_OPTIONS = {
            "Morning",
            "Afternoon",
            "Evening",
            "Night",
            "Weekdays",
            "Weekends",
            "Weekday Evenings",
            "Weekend Mornings",
            "Weekend Evenings"
    };

    public static List<String> getSkillList() {
        return Arrays.asList(PREDEFINED_SKILLS);
    }
}
