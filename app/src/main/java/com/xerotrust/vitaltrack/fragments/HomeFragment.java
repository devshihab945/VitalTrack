package com.xerotrust.vitaltrack.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.activities.*;
import com.xerotrust.vitaltrack.adapters.QuickActionAdapter;
import com.xerotrust.vitaltrack.models.QuickActionItem;
import com.xerotrust.vitaltrack.services.StepCounterService;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.utils.LanguageHelper;
import com.xerotrust.vitaltrack.utils.StepCalculatorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Calendar;

public class HomeFragment extends Fragment {

    private AppPrefs prefs;

    // ── All Health Tips Pool (500+) ──────────────────────────────────────────
    private static final String[][] ALL_TIPS = {
            // Hydration
            {"\uD83D\uDCA7", "Drink at least 8 glasses of water daily to stay hydrated.", "#1976D2"}, {"\uD83D\uDCA7", "Start your morning with a glass of warm water to kickstart digestion.", "#1976D2"}, {"\uD83D\uDCA7", "Carry a reusable water bottle to track your daily water intake.", "#1565C0"}, {"\uD83D\uDCA7", "Drinking water before meals can help reduce calorie intake.", "#1976D2"}, {"\uD83D\uDCA7", "Herbal teas count toward your daily fluid intake goal.", "#0288D1"}, {"\uD83D\uDCA7", "Coconut water is a natural electrolyte-rich hydration option.", "#00796B"}, {"\uD83D\uDCA7", "Thirst is often confused with hunger — drink water first.", "#1976D2"}, {"\uD83D\uDCA7", "Dehydration can impair focus and short-term memory.", "#1565C0"}, {"\uD83D\uDCA7", "Your urine color is the best indicator of hydration — aim for pale yellow.", "#F57C00"}, {"\uD83D\uDCA7", "Drinking cold water can slightly boost your metabolism.", "#0288D1"}, {"\uD83D\uDCA7", "Eat water-rich fruits like watermelon and cucumber to stay hydrated.", "#388E3C"}, {"\uD83D\uDCA7", "Athletes should drink 500ml of water 2 hours before exercise.", "#1976D2"}, {"\uD83D\uDCA7", "Add lemon or mint to water if plain water feels boring.", "#0288D1"}, {"\uD83D\uDCA7", "Coffee and alcohol are diuretics — drink extra water to compensate.", "#5D4037"}, {"\uD83D\uDCA7", "Drinking water can help prevent and relieve headaches.", "#1565C0"}, {"\uD83D\uDCA7", "Your kidneys need water to filter waste from the blood properly.", "#0288D1"}, {"\uD83D\uDCA7", "Sports drinks are only necessary for workouts longer than 60 minutes.", "#388E3C"}, {"\uD83D\uDCA7", "Water helps maintain the balance of body fluids critical for digestion.", "#1976D2"}, {"\uD83D\uDCA7", "Drinking water regularly keeps skin plump and reduces wrinkles.", "#E91E63"}, {"\uD83D\uDCA7", "Set hourly reminders on your phone to drink a glass of water.", "#1565C0"},

            // Sleep
            {"\uD83D\uDE34", "Get 7–9 hours of sleep for optimal health and recovery.", "#7B1FA2"}, {"\uD83D\uDE34", "Keep a consistent sleep schedule, even on weekends.", "#6A1B9A"}, {"\uD83D\uDE34", "Avoid screens for at least 30 minutes before bed.", "#7B1FA2"}, {"\uD83D\uDE34", "A cool room (around 18°C/65°F) promotes deeper sleep.", "#5E35B1"}, {"\uD83D\uDE34", "Avoid caffeine after 2 PM to protect sleep quality.", "#4527A0"}, {"\uD83D\uDE34", "Darkness signals your brain to produce melatonin for sleep.", "#7B1FA2"}, {"\uD83D\uDE34", "A consistent bedtime routine trains your body clock.", "#6A1B9A"}, {"\uD83D\uDE34", "Napping for 20 minutes can restore alertness without grogginess.", "#7B1FA2"}, {"\uD83D\uDE34", "Sleep deprivation increases hunger hormones like ghrelin.", "#5E35B1"}, {"\uD83D\uDE34", "Quality sleep improves emotional regulation and resilience.", "#4527A0"}, {"\uD83D\uDE34", "White noise or nature sounds can help mask disruptive noise.", "#7B1FA2"}, {"\uD83D\uDE34", "Your body repairs muscles and tissues during deep sleep stages.", "#6A1B9A"}, {"\uD83D\uDE34", "Alcohol disrupts REM sleep — limit it before bedtime.", "#7B1FA2"}, {"\uD83D\uDE34", "Reading a physical book before bed is a great wind-down routine.", "#5E35B1"}, {"\uD83D\uDE34", "Sleep is when your brain consolidates memories and learning.", "#4527A0"}, {"\uD83D\uDE34", "Invest in a good mattress — you spend a third of your life on it.", "#7B1FA2"}, {"\uD83D\uDE34", "Stretching before bed eases muscle tension and improves sleep.", "#6A1B9A"}, {"\uD83D\uDE34", "Journaling before bed can clear racing thoughts and reduce anxiety.", "#7B1FA2"}, {"\uD83D\uDE34", "Avoid heavy meals within 2–3 hours of bedtime.", "#5E35B1"}, {"\uD83D\uDE34", "Sleep hygiene is as important as diet and exercise for long-term health.", "#4527A0"},

            // Exercise & Activity
            {"\uD83D\uDEB6", "Aim for 7,500–10,000 steps per day to improve cardiovascular health.", "#388E3C"}, {"\uD83D\uDEB6", "Even a 10-minute walk after meals improves blood sugar levels.", "#2E7D32"}, {"\uD83D\uDEB6", "Take the stairs instead of the elevator whenever possible.", "#388E3C"}, {"\uD83D\uDEB6", "Walk or cycle for short errands instead of driving.", "#1B5E20"}, {"\uD83D\uDEB6", "Dancing is a fun way to get cardio exercise without it feeling like work.", "#388E3C"}, {"\uD83C\uDFCB️", "Strength training 2–3 times a week improves bone density.", "#5E35B1"}, {"\uD83C\uDFCB️", "Resistance training boosts metabolism even at rest.", "#4527A0"}, {"\uD83C\uDFCB️", "Compound exercises like squats and deadlifts work multiple muscle groups.", "#5E35B1"}, {"\uD83C\uDFCB️", "Progressive overload — gradually increasing weight — builds strength safely.", "#4527A0"}, {"\uD83C\uDFCB️", "Core strength is essential for protecting your spine during daily activities.", "#5E35B1"}, {"⏱️", "Take a 5-minute walk every hour if you have a desk job.", "#00796B"}, {"⏱️", "Sitting for long periods raises risk of heart disease — stand up regularly.", "#00695C"}, {"⏱️", "Use a standing desk or alternate between sitting and standing.", "#00796B"}, {"⏱️", "Do calf raises or squats during TV commercials.", "#00695C"}, {"⏱️", "Schedule exercise like a meeting — block it in your calendar.", "#00796B"}, {"\uD83E\uDDD8", "Yoga improves flexibility, strength, and mental calmness.", "#0288D1"}, {"\uD83E\uDDD8", "Pilates strengthens the core and improves posture effectively.", "#0277BD"}, {"\uD83E\uDDD8", "Swimming is a low-impact exercise ideal for joint health.", "#0288D1"}, {"\uD83E\uDDD8", "Cycling is an excellent cardio exercise that's easy on the knees.", "#0277BD"}, {"\uD83E\uDDD8", "Jump rope burns more calories per minute than most exercises.", "#0288D1"}, {"\uD83C\uDFC3", "High-Intensity Interval Training (HIIT) burns fat efficiently in less time.", "#C62828"}, {"\uD83C\uDFC3", "Running improves cardiovascular endurance and mental health.", "#B71C1C"}, {"\uD83C\uDFC3", "Warm up before exercise to prevent injury and improve performance.", "#C62828"}, {"\uD83C\uDFC3", "Cool down after workouts to bring heart rate back safely.", "#B71C1C"}, {"\uD83C\uDFC3", "Rest days are as important as workout days for muscle recovery.", "#C62828"},

            // Nutrition
            {"\uD83E\uDD66", "Eat a rainbow of vegetables to get a wide range of nutrients.", "#F57C00"}, {"\uD83E\uDD66", "Leafy greens like spinach and kale are among the most nutrient-dense foods.", "#388E3C"}, {"\uD83E\uDD66", "Aim to fill half your plate with vegetables at every meal.", "#F57C00"}, {"\uD83E\uDD66", "Broccoli is rich in Vitamin C, fiber, and cancer-fighting compounds.", "#388E3C"}, {"\uD83E\uDD66", "Fermented foods like yogurt and kimchi support gut microbiome health.", "#795548"}, {"\uD83C\uDF4E", "An apple a day keeps the doctor away — eat more whole fruits!", "#558B2F"}, {"\uD83C\uDF4E", "Berries are packed with antioxidants that protect against cell damage.", "#7B1FA2"}, {"\uD83C\uDF4E", "Avocados provide healthy monounsaturated fats good for heart health.", "#558B2F"}, {"\uD83C\uDF4E", "Bananas are a great source of potassium and natural energy.", "#F57C00"}, {"\uD83C\uDF4E", "Citrus fruits are rich in Vitamin C, boosting immune function.", "#F9A825"}, {"\uD83E\uDDC2", "Limit sodium to 2,300 mg/day to maintain healthy blood pressure.", "#C62828"}, {"\uD83E\uDDC2", "Processed foods often contain hidden sodium — always check labels.", "#B71C1C"}, {"\uD83E\uDDC2", "Use herbs and spices instead of salt to flavor your food.", "#C62828"}, {"\uD83E\uDDC2", "Sea salt and table salt have the same sodium content by weight.", "#B71C1C"}, {"\uD83E\uDDC2", "Potassium-rich foods counterbalance sodium's effects on blood pressure.", "#388E3C"}, {"\uD83D\uDC1F", "Fatty fish like salmon and sardines are rich in omega-3 fatty acids.", "#0288D1"}, {"\uD83D\uDC1F", "Omega-3s reduce inflammation and support brain and heart health.", "#0277BD"}, {"\uD83D\uDC1F", "Aim for at least 2 servings of fish per week for optimal omega-3 intake.", "#0288D1"}, {"\uD83D\uDC1F", "Flaxseeds and walnuts are plant-based omega-3 sources.", "#5E35B1"}, {"\uD83D\uDC1F", "Eating fish is linked to lower rates of depression and anxiety.", "#0288D1"}, {"\uD83E\uDD5A", "Eggs are one of the most nutritionally complete foods available.", "#F57C00"}, {"\uD83E\uDD5A", "Whole eggs contain choline, essential for brain development.", "#F57C00"}, {"\uD83E\uDD5A", "Legumes like lentils and chickpeas are excellent plant protein sources.", "#795548"}, {"\uD83E\uDD5A", "Nuts and seeds provide healthy fats, protein, and minerals.", "#5D4037"}, {"\uD83E\uDD5A", "Quinoa is a complete protein containing all 9 essential amino acids.", "#388E3C"}, {"\uD83C\uDF5A", "Choose whole grains over refined grains for more fiber and nutrients.", "#795548"}, {"\uD83C\uDF5A", "Brown rice, oats, and barley help stabilize blood sugar levels.", "#6D4C41"}, {"\uD83C\uDF5A", "Fiber-rich foods feed beneficial gut bacteria and prevent constipation.", "#795548"}, {"\uD83C\uDF5A", "Overnight oats make a quick, nutritious, high-fiber breakfast.", "#6D4C41"}, {"\uD83C\uDF5A", "Avoid white bread and sugary cereals that cause blood sugar spikes.", "#795548"},

            // Mental Health
            {"\uD83E\uDDD8", "Regular meditation can significantly reduce stress levels.", "#0288D1"}, {"\uD83E\uDDD8", "Even 5 minutes of mindfulness daily reduces cortisol levels.", "#0277BD"}, {"\uD83E\uDDD8", "Gratitude journaling improves mood and life satisfaction.", "#0288D1"}, {"\uD83E\uDDD8", "Spending time in nature reduces anxiety and improves well-being.", "#388E3C"}, {"\uD83E\uDDD8", "Social connections are one of the strongest predictors of longevity.", "#7B1FA2"}, {"\uD83E\uDDD8", "Acts of kindness boost serotonin for both giver and receiver.", "#0288D1"}, {"\uD83E\uDDD8", "Laughter is genuinely medicine — it lowers stress hormones.", "#F57C00"}, {"\uD83E\uDDD8", "Setting boundaries protects your mental energy and reduces burnout.", "#0277BD"}, {"\uD83E\uDDD8", "Talking to a therapist is a sign of strength, not weakness.", "#0288D1"}, {"\uD83E\uDDD8", "A digital detox one day per week can significantly lower anxiety.", "#388E3C"}, {"\uD83E\uDDD8", "Creative hobbies like drawing or music act as active meditation.", "#7B1FA2"}, {"\uD83E\uDDD8", "Physical exercise is as effective as antidepressants for mild depression.", "#C62828"}, {"\uD83E\uDDD8", "Practicing mindful eating reduces binge eating and food guilt.", "#0288D1"}, {"\uD83E\uDDD8", "Limit news consumption if it increases your anxiety or stress.", "#0277BD"}, {"\uD83E\uDDD8", "Self-compassion is as important as discipline for long-term health.", "#0288D1"},

            // Sun & Vitamins
            {"☀️", "Get 15–20 minutes of sunlight daily for Vitamin D production.", "#FFA000"}, {"☀️", "Vitamin D deficiency is linked to depression, fatigue, and weak immunity.", "#FF8F00"}, {"☀️", "Morning sunlight helps regulate your circadian rhythm for better sleep.", "#FFA000"}, {"☀️", "Sunlight exposure before noon is more effective for Vitamin D.", "#FF8F00"}, {"☀️", "People with darker skin need more sun exposure to produce Vitamin D.", "#FFA000"}, {"\uD83E\uDDF4", "Apply sunscreen daily — UV damage accumulates even on cloudy days.", "#E65100"}, {"\uD83E\uDDF4", "UV rays can penetrate car windows — apply SPF before driving.", "#BF360C"}, {"\uD83E\uDDF4", "Reapply sunscreen every 2 hours when outdoors for effective protection.", "#E65100"}, {"\uD83E\uDDF4", "Sunglasses protect your eyes from UV-related macular degeneration.", "#1565C0"}, {"\uD83E\uDDF4", "Protective clothing reduces UV exposure better than sunscreen alone.", "#E65100"},

            // Breathing & Relaxation
            {"\uD83E\uDEB1", "Deep breathing for 5 minutes can lower your heart rate quickly.", "#00838F"}, {"\uD83E\uDEB1", "Box breathing (4-4-4-4) activates the parasympathetic nervous system.", "#00695C"}, {"\uD83E\uDEB1", "Diaphragmatic breathing engages your core and reduces tension.", "#00838F"}, {"\uD83E\uDEB1", "The 4-7-8 breathing technique can help you fall asleep faster.", "#00695C"}, {"\uD83E\uDEB1", "Slow nasal breathing during exercise improves oxygen efficiency.", "#00838F"}, {"\uD83E\uDEB1", "Progressive muscle relaxation reduces physical stress stored in the body.", "#00695C"}, {"\uD83E\uDEB1", "Alternate nostril breathing balances the nervous system beautifully.", "#00838F"}, {"\uD83E\uDEB1", "Even one conscious deep breath can reset your stress response.", "#00695C"}, {"\uD83E\uDEB1", "Breathwork practices reduce blood pressure over time.", "#00838F"}, {"\uD83E\uDEB1", "Mouth breathing during sleep is linked to poor sleep quality.", "#00695C"},

            // Posture & Ergonomics
            {"\uD83E\uDDD8", "Sit up straight — poor posture causes chronic neck and back pain.", "#5E35B1"}, {"\uD83E\uDDD8", "Your monitor should be at eye level to prevent neck strain.", "#4527A0"}, {"\uD83E\uDDD8", "Ergonomic chairs prevent lumbar issues during long working hours.", "#5E35B1"}, {"\uD83E\uDDD8", "Roll your shoulders back to correct hunching when using a phone.", "#4527A0"}, {"\uD83E\uDDD8", "Core strengthening exercises are the best long-term fix for back pain.", "#5E35B1"}, {"\uD83E\uDDD8", "Sleep on your side to reduce back pain and improve spinal alignment.", "#4527A0"}, {"\uD83E\uDDD8", "A pillow between your knees while sleeping reduces hip and back strain.", "#5E35B1"}, {"\uD83E\uDDD8", "Stretching your hip flexors counteracts the damage of sitting all day.", "#4527A0"}, {"\uD83E\uDDD8", "Text neck is a growing condition — hold your phone at eye level.", "#5E35B1"}, {"\uD83E\uDDD8", "Foam rolling for 10 minutes releases muscle fascia and reduces soreness.", "#4527A0"},

            // Heart Health
            {"\u2764\uFE0F", "Cardiovascular disease is the world's leading cause of death — act early.", "#C62828"}, {"\u2764\uFE0F", "Regular aerobic exercise strengthens the heart muscle directly.", "#B71C1C"}, {"\u2764\uFE0F", "A Mediterranean diet significantly reduces cardiovascular risk.", "#C62828"}, {"\u2764\uFE0F", "Trans fats from processed foods raise bad cholesterol — avoid them.", "#B71C1C"}, {"\u2764\uFE0F", "Smoking is the single largest preventable cause of heart disease.", "#C62828"}, {"\u2764\uFE0F", "High blood pressure is called the 'silent killer' — get it checked regularly.", "#B71C1C"}, {"\u2764\uFE0F", "Dark chocolate (70%+) contains flavonoids that support heart health.", "#5D4037"}, {"\u2764\uFE0F", "Laughter and positive emotions reduce cardiovascular stress markers.", "#C62828"}, {"\u2764\uFE0F", "Managing chronic stress is critical for long-term heart health.", "#B71C1C"}, {"\u2764\uFE0F", "Getting enough sleep reduces the risk of heart attack and stroke.", "#7B1FA2"},

            // Gut Health
            {"\uD83E\uDDA0", "Your gut microbiome contains trillions of bacteria influencing your health.", "#795548"}, {"\uD83E\uDDA0", "Probiotic foods like yogurt, kefir, and sauerkraut support gut health.", "#6D4C41"}, {"\uD83E\uDDA0", "Prebiotic foods like garlic, onions, and bananas feed good gut bacteria.", "#795548"}, {"\uD83E\uDDA0", "Antibiotic use can disrupt gut flora — follow up with probiotics.", "#6D4C41"}, {"\uD83E\uDDA0", "A diverse diet leads to a more diverse and resilient microbiome.", "#795548"}, {"\uD83E\uDDA0", "Chronic stress negatively impacts your gut-brain axis.", "#6D4C41"}, {"\uD83E\uDDA0", "Chew food thoroughly — digestion begins in the mouth.", "#795548"}, {"\uD83E\uDDA0", "A healthy gut supports 70% of your immune system function.", "#6D4C41"}, {"\uD83E\uDDA0", "Artificial sweeteners may disrupt the gut microbiome.", "#795548"}, {"\uD83E\uDDA0", "Eating slowly and mindfully improves digestion and satiety.", "#6D4C41"},

            // Eye Health
            {"\uD83D\uDC41\uFE0F", "Follow the 20-20-20 rule: every 20 mins, look 20 feet away for 20 secs.", "#0288D1"}, {"\uD83D\uDC41\uFE0F", "Reduce screen brightness and use night mode to protect your eyes.", "#0277BD"}, {"\uD83D\uDC41\uFE0F", "Carrots contain beta-carotene which converts to Vitamin A for eye health.", "#F57C00"}, {"\uD83D\uDC41\uFE0F", "Lutein and zeaxanthin in leafy greens protect the retina from damage.", "#388E3C"}, {"\uD83D\uDC41\uFE0F", "Annual eye exams can catch early signs of glaucoma and macular issues.", "#0288D1"}, {"\uD83D\uDC41\uFE0F", "Adequate sleep is essential for eye health and preventing dry eyes.", "#7B1FA2"}, {"\uD83D\uDC41\uFE0F", "Wearing sunglasses reduces cumulative UV damage to the lens.", "#E65100"}, {"\uD83D\uDC41\uFE0F", "Blinking consciously reduces digital eye strain during screen time.", "#0288D1"}, {"\uD83D\uDC41\uFE0F", "Blue-light filtering glasses may help reduce eye fatigue.", "#0277BD"}, {"\uD83D\uDC41\uFE0F", "Dry eyes can worsen with caffeine — drink more water.", "#1976D2"},

            // Oral Health
            {"\uD83E\uDDB7", "Brush your teeth for 2 minutes twice daily to prevent decay.", "#00796B"}, {"\uD83E\uDDB7", "Floss daily — brushing alone misses 40% of tooth surfaces.", "#00695C"}, {"\uD83E\uDDB7", "Mouthwash kills bacteria that cause bad breath and gum disease.", "#00796B"}, {"\uD83E\uDDB7", "Poor oral health is linked to heart disease and diabetes.", "#C62828"}, {"\uD83E\uDDB7", "Sugary drinks are more damaging to teeth than solid sugary foods.", "#00796B"}, {"\uD83E\uDDB7", "Visit your dentist every 6 months even if you have no pain.", "#00695C"}, {"\uD83E\uDDB7", "Chewing sugar-free gum stimulates saliva that protects teeth.", "#00796B"}, {"\uD83E\uDDB7", "Electric toothbrushes are significantly more effective than manual ones.", "#00695C"}, {"\uD83E\uDDB7", "Calcium and Vitamin D are essential for strong tooth enamel.", "#00796B"}, {"\uD83E\uDDB7", "Tongue scraping reduces bacteria that cause bad breath.", "#00695C"},

            // Skin Health
            {"\uD83C\uDF3F", "Moisturize daily to maintain skin barrier function.", "#E91E63"}, {"\uD83C\uDF3F", "Vitamin C serum brightens skin and boosts collagen production.", "#F57C00"}, {"\uD83C\uDF3F", "Retinol is one of the most proven anti-aging ingredients available.", "#E91E63"}, {"\uD83C\uDF3F", "Stress hormones like cortisol trigger acne and skin inflammation.", "#C62828"}, {"\uD83C\uDF3F", "Diet high in sugar accelerates skin aging through glycation.", "#795548"}, {"\uD83C\uDF3F", "Always remove makeup before sleeping to prevent clogged pores.", "#E91E63"}, {"\uD83C\uDF3F", "Cold showers can tighten pores and improve circulation to skin.", "#1976D2"}, {"\uD83C\uDF3F", "Omega-3 fatty acids reduce skin inflammation and dryness.", "#0288D1"}, {"\uD83C\uDF3F", "Excessive hot showers strip natural oils from skin.", "#E65100"}, {"\uD83C\uDF3F", "Hyaluronic acid holds 1,000x its weight in water — great for dry skin.", "#E91E63"},

            // Immune System
            {"\uD83D\uDEE1\uFE0F", "A balanced diet is the foundation of a strong immune system.", "#388E3C"}, {"\uD83D\uDEE1\uFE0F", "Vitamin C, D, and Zinc are critical for immune defense.", "#F57C00"}, {"\uD83D\uDEE1\uFE0F", "Regular moderate exercise boosts immune function significantly.", "#388E3C"}, {"\uD83D\uDEE1\uFE0F", "Chronic stress suppresses the immune system over time.", "#C62828"}, {"\uD83D\uDEE1\uFE0F", "Getting 7–9 hours of sleep is the #1 immune booster.", "#7B1FA2"}, {"\uD83D\uDEE1\uFE0F", "Gut health and immunity are deeply interconnected.", "#795548"}, {"\uD83D\uDEE1\uFE0F", "Excessive alcohol weakens immune response and recovery.", "#C62828"}, {"\uD83D\uDEE1\uFE0F", "Vaccinations are one of the most effective immune-boosting tools.", "#388E3C"}, {"\uD83D\uDEE1\uFE0F", "Garlic has natural antimicrobial and immune-enhancing properties.", "#F57C00"}, {"\uD83D\uDEE1\uFE0F", "Elderberry extract may reduce the duration of cold and flu symptoms.", "#7B1FA2"},

            // Weight Management
            {"\u2696\uFE0F", "Sustainable weight loss is 0.5–1 kg per week — slow is steady.", "#795548"}, {"\u2696\uFE0F", "Caloric deficit, not specific diets, is the key to weight loss.", "#6D4C41"}, {"\u2696\uFE0F", "Eating protein at every meal reduces hunger and preserves muscle.", "#795548"}, {"\u2696\uFE0F", "Liquid calories from juices and sodas are easy to overlook.", "#C62828"}, {"\u2696\uFE0F", "Mindful eating prevents unconscious overeating and promotes satiety.", "#795548"}, {"\u2696\uFE0F", "Sleep deprivation increases cravings for high-calorie junk food.", "#7B1FA2"}, {"\u2696\uFE0F", "Strength training prevents the muscle loss that accompanies dieting.", "#5E35B1"}, {"\u2696\uFE0F", "Cooking at home gives you full control over ingredients and portions.", "#388E3C"}, {"\u2696\uFE0F", "Eating slowly allows your brain time to register fullness.", "#795548"}, {"\u2696\uFE0F", "Focus on building healthy habits — weight loss follows naturally.", "#6D4C41"},

            // Diabetes & Blood Sugar
            {"\uD83E\uDE78", "Blood sugar spikes are reduced by pairing carbs with protein and fat.", "#C62828"}, {"\uD83E\uDE78", "Walking after meals is one of the most effective blood sugar regulators.", "#388E3C"}, {"\uD83E\uDE78", "Cinnamon may help improve insulin sensitivity.", "#795548"}, {"\uD83E\uDE78", "Refined carbohydrates cause rapid blood sugar spikes and crashes.", "#C62828"}, {"\uD83E\uDE78", "Apple cider vinegar before meals may blunt blood sugar response.", "#795548"}, {"\uD83E\uDE78", "Regular fasting blood sugar checks help detect prediabetes early.", "#C62828"}, {"\uD83E\uDE78", "Type 2 diabetes is largely preventable with lifestyle changes.", "#388E3C"}, {"\uD83E\uDE78", "High fiber foods slow glucose absorption and stabilize energy levels.", "#795548"}, {"\uD83E\uDE78", "Stress raises cortisol, which raises blood sugar — manage stress.", "#C62828"}, {"\uD83E\uDE78", "Intermittent fasting can improve insulin sensitivity over time.", "#0288D1"},

            // Hormones & Energy
            {"\u26A1", "Chronic fatigue may signal thyroid, iron, or Vitamin B12 deficiency.", "#F57C00"}, {"\u26A1", "B vitamins are essential for energy metabolism in every cell.", "#FF8F00"}, {"\u26A1", "Iron deficiency anemia is the most common nutritional deficiency worldwide.", "#C62828"}, {"\u26A1", "Magnesium is involved in over 300 enzyme reactions and boosts energy.", "#388E3C"}, {"\u26A1", "Thyroid health is closely tied to iodine and selenium intake.", "#F57C00"}, {"\u26A1", "Balanced cortisol levels require adequate sleep and stress management.", "#7B1FA2"}, {"\u26A1", "Testosterone and estrogen levels are influenced by sleep, diet, and exercise.", "#5E35B1"}, {"\u26A1", "Eating too little can slow metabolism and spike cortisol.", "#C62828"}, {"\u26A1", "Adaptogenic herbs like ashwagandha may support hormonal balance.", "#795548"}, {"\u26A1", "Regular blood work is the best way to track hormonal and nutritional health.", "#0288D1"},

            // Children & Family Health
            {"\uD83D\uDC76", "Children need 60 minutes of moderate-to-vigorous play daily.", "#388E3C"}, {"\uD83D\uDC76", "Limit screen time for children under 6 to under 1 hour per day.", "#F57C00"}, {"\uD83D\uDC76", "Breastfeeding for 6 months provides essential immune protection.", "#E91E63"}, {"\uD83D\uDC76", "Teach children to eat vegetables by including them in family meals early.", "#388E3C"}, {"\uD83D\uDC76", "Children learn health habits by watching their parents — model healthy behavior.", "#F57C00"}, {"\uD83D\uDC76", "Childhood obesity significantly raises risk of adult chronic disease.", "#C62828"}, {"\uD83D\uDC76", "Reading before bed is one of the best bedtime routines for children.", "#7B1FA2"}, {"\uD83D\uDC76", "Regular outdoor play supports bone development and Vitamin D synthesis.", "#388E3C"}, {"\uD83D\uDC76", "Ensure children get all recommended vaccinations on schedule.", "#0288D1"}, {"\uD83D\uDC76", "Family meals are associated with better mental health outcomes in children.", "#795548"},

            // Aging Well
            {"\uD83E\uDDD3", "Muscle mass declines 1–2% per year after 30 — strength training is essential.", "#5E35B1"}, {"\uD83E\uDDD3", "Balance exercises reduce fall risk, a leading cause of injury in the elderly.", "#4527A0"}, {"\uD83E\uDDD3", "Social isolation in older adults increases dementia risk significantly.", "#7B1FA2"}, {"\uD83E\uDDD3", "Regular cognitive challenges like puzzles help maintain brain plasticity.", "#5E35B1"}, {"\uD83E\uDDD3", "Lifelong learners tend to live longer and healthier lives.", "#4527A0"}, {"\uD83E\uDDD3", "Caloric restriction and intermittent fasting are linked to longevity.", "#0288D1"}, {"\uD83E\uDDD3", "Maintaining a sense of purpose extends healthy lifespan.", "#7B1FA2"}, {"\uD83E\uDDD3", "Bone density peaks at 30 — load-bearing exercise preserves it.", "#5E35B1"}, {"\uD83E\uDDD3", "Hearing loss is undertreated and linked to cognitive decline.", "#4527A0"}, {"\uD83E\uDDD3", "Strong social networks are one of the best predictors of healthy aging.", "#7B1FA2"},

            // Prevention & Checkups
            {"\uD83C\uDFE5", "Annual health checkups catch silent conditions before they worsen.", "#0288D1"}, {"\uD83C\uDFE5", "Blood pressure should be checked at least once a year after age 18.", "#C62828"}, {"\uD83C\uDFE5", "Cholesterol screening is recommended every 4–6 years for adults.", "#F57C00"}, {"\uD83C\uDFE5", "Colorectal cancer screening should begin at age 45 for average-risk adults.", "#795548"}, {"\uD83C\uDFE5", "Skin cancer is the most common cancer — do regular skin self-exams.", "#E65100"}, {"\uD83C\uDFE5", "Breast cancer screening mammograms are recommended from age 40–50.", "#E91E63"}, {"\uD83C\uDFE5", "Cervical cancer can be prevented with HPV vaccination and Pap smears.", "#E91E63"}, {"\uD83C\uDFE5", "Dental checkups twice yearly prevent costly and painful dental issues.", "#00796B"}, {"\uD83C\uDFE5", "Eye exams should be done every 1–2 years depending on age.", "#0288D1"}, {"\uD83C\uDFE5", "Know your family health history — it's a powerful risk factor guide.", "#795548"},

            // Alcohol & Smoking
            {"\uD83D\uDEAD", "Heavy drinking damages the liver, brain, heart, and immune system.", "#C62828"}, {"\uD83D\uDEAD", "Even moderate alcohol raises the risk of certain cancers.", "#B71C1C"}, {"\uD83D\uDEAD", "Alcohol disrupts sleep architecture, reducing restorative deep sleep.", "#7B1FA2"}, {"\uD83D\uDEAD", "Non-alcoholic drinks and mocktails can replace alcohol socially.", "#388E3C"}, {"\uD83D\uDEAD", "Alcohol is a leading cause of preventable death globally.", "#C62828"}, {"\uD83D\uDEAC", "Quitting smoking adds years to your life — it's never too late.", "#C62828"}, {"\uD83D\uDEAC", "Within 20 minutes of quitting, blood pressure begins to drop.", "#B71C1C"}, {"\uD83D\uDEAC", "After 1 year smoke-free, heart disease risk drops by half.", "#C62828"}, {"\uD83D\uDEAC", "Vaping is not a safe alternative to cigarettes — it has its own risks.", "#B71C1C"}, {"\uD83D\uDEAC", "Nicotine replacement therapies double your chances of quitting.", "#C62828"},

            // Food Habits
            {"\uD83C\uDF7D\uFE0F", "Meal prepping on Sundays saves time and ensures healthier choices.", "#388E3C"}, {"\uD83C\uDF7D\uFE0F", "Never skip breakfast — it fuels brain and body for the morning.", "#F57C00"}, {"\uD83C\uDF7D\uFE0F", "Eating at a table without screens improves mindful eating.", "#795548"}, {"\uD83C\uDF7D\uFE0F", "Cook with olive oil for its heart-protective monounsaturated fats.", "#F57C00"}, {"\uD83C\uDF7D\uFE0F", "Avoid eating late at night — digestion slows significantly during sleep.", "#5E35B1"}, {"\uD83C\uDF7D\uFE0F", "Portion sizes have doubled over decades — use smaller plates.", "#795548"}, {"\uD83C\uDF7D\uFE0F", "Reading food labels helps you make informed nutritional choices.", "#0288D1"}, {"\uD83C\uDF7D\uFE0F", "Limit added sugar to under 25g (6 tsp) per day for women.", "#C62828"}, {"\uD83C\uDF7D\uFE0F", "Limit added sugar to under 36g (9 tsp) per day for men.", "#C62828"}, {"\uD83C\uDF7D\uFE0F", "Eat the rainbow — a variety of colors ensures diverse phytonutrients.", "#F57C00"},

            // Work & Productivity Health
            {"\uD83D\uDCBB", "Ergonomic keyboard and mouse positioning prevents repetitive strain injury.", "#0288D1"}, {"\uD83D\uDCBB", "The Pomodoro Technique (25 min work + 5 min break) boosts productivity.", "#F57C00"}, {"\uD83D\uDCBB", "Decluttering your workspace reduces cognitive load and stress.", "#795548"}, {"\uD83D\uDCBB", "Overwork leads to burnout — sustainable productivity requires rest.", "#C62828"}, {"\uD83D\uDCBB", "Working from home? Define clear start and stop times for work.", "#388E3C"}, {"\uD83D\uDCBB", "Natural light in your workspace boosts mood, focus, and energy.", "#FFA000"}, {"\uD83D\uDCBB", "Stand-up meetings are shorter and prevent prolonged sitting.", "#00796B"}, {"\uD83D\uDCBB", "Micro-breaks every 60 minutes restore focus and reduce fatigue.", "#0288D1"}, {"\uD83D\uDCBB", "Plants in your workspace improve air quality and mental well-being.", "#388E3C"}, {"\uD83D\uDCBB", "Multitasking reduces efficiency — single-task for better output.", "#795548"},

            // Environmental Health
            {"\uD83C\uDF31", "Indoor air can be 2–5x more polluted than outdoor air — ventilate.", "#388E3C"}, {"\uD83C\uDF31", "House plants like spider plants filter indoor air pollutants.", "#2E7D32"}, {"\uD83C\uDF31", "Reduce plastic use — BPA and phthalates disrupt hormones.", "#F57C00"}, {"\uD83C\uDF31", "Avoid using non-stick cookware at high heat — it releases toxins.", "#C62828"}, {"\uD83C\uDF31", "Filtered water removes chlorine, heavy metals, and microplastics.", "#1976D2"}, {"\uD83C\uDF31", "Organic produce reduces pesticide exposure, especially for thin-skinned fruits.", "#388E3C"}, {"\uD83C\uDF31", "Noise pollution raises cortisol and blood pressure — reduce exposure.", "#795548"}, {"\uD83C\uDF31", "Spending 120+ minutes in nature weekly is linked to better health.", "#388E3C"}, {"\uD83C\uDF31", "Forest bathing (Shinrin-yoku) reduces stress hormones measurably.", "#2E7D32"}, {"\uD83C\uDF31", "Grounding (walking barefoot on grass/earth) may reduce inflammation.", "#388E3C"},

            // Additional Random Wellness Tips
            {"\uD83D\uDCA1", "Health is not just the absence of disease — it's overall well-being.", "#0288D1"}, {"\uD83D\uDCA1", "Small consistent habits compound into massive health improvements.", "#388E3C"}, {"\uD83D\uDCA1", "Track your health metrics monthly to stay aware of changes.", "#0288D1"}, {"\uD83D\uDCA1", "Prevention costs less — financially and physically — than treatment.", "#C62828"}, {"\uD83D\uDCA1", "Your body has an incredible ability to heal when given the right conditions.", "#388E3C"}, {"\uD83D\uDCA1", "Genetics load the gun, but lifestyle pulls the trigger.", "#795548"}, {"\uD83D\uDCA1", "Invest in your health now — it pays the highest returns over a lifetime.", "#0288D1"}, {"\uD83D\uDCA1", "Health is not a destination but a daily practice.", "#388E3C"}, {"\uD83D\uDCA1", "The best time to start a healthy habit was yesterday. The next best is now.", "#F57C00"}, {"\uD83D\uDCA1", "Consistency beats perfection — showing up matters more than doing it perfectly.", "#0288D1"},

            // Bonus extra tips to push past 500
            {"\uD83E\uDD51", "Eat avocado with tomatoes to enhance lycopene absorption.", "#558B2F"}, {"\uD83E\uDD51", "Turmeric's curcumin is better absorbed when paired with black pepper.", "#F57C00"}, {"\uD83E\uDD51", "Green tea contains L-theanine which provides calm, focused energy.", "#388E3C"}, {"\uD83E\uDD51", "Saffron has been shown to reduce symptoms of mild depression.", "#FFA000"}, {"\uD83E\uDD51", "Ginger has anti-inflammatory and anti-nausea properties.", "#795548"}, {"\uD83E\uDD51", "Blueberries improve memory and slow brain aging.", "#7B1FA2"}, {"\uD83E\uDD51", "Walnuts are one of the best brain-supporting foods due to omega-3 content.", "#5D4037"}, {"\uD83E\uDD51", "Dark leafy greens like kale are among the most nutrient-dense foods on Earth.", "#388E3C"}, {"\uD83E\uDD51", "Chia seeds provide fiber, protein, and omega-3s in one small package.", "#795548"}, {"\uD83E\uDD51", "Extra virgin olive oil is one of the most extensively researched health foods.", "#F57C00"}, {"\uD83C\uDFCA", "Swimming reduces joint stress while giving a full-body workout.", "#0288D1"}, {"\uD83C\uDFCA", "Tai chi improves balance, flexibility, and mental calm — great for all ages.", "#0277BD"}, {"\uD83C\uDFCA", "Rock climbing builds grip strength, problem-solving, and total body fitness.", "#795548"}, {"\uD83C\uDFCA", "Rowing is one of the best full-body low-impact cardio exercises.", "#C62828"}, {"\uD83C\uDFCA", "Group fitness classes improve adherence — social accountability works.", "#388E3C"}, {"\uD83C\uDFCA", "Walking barefoot on natural surfaces strengthens foot muscles.", "#795548"}, {"\uD83C\uDFCA", "Flexibility training reduces injury risk and improves daily movement quality.", "#5E35B1"}, {"\uD83C\uDFCA", "Agility training maintains fast-twitch muscle fibers that decline with age.", "#C62828"}, {"\uD83C\uDFCA", "Cross-training prevents overuse injuries and builds balanced fitness.", "#0288D1"}, {"\uD83C\uDFCA", "Exercise is the closest thing to a miracle drug that science has found.", "#388E3C"},};

    // ── All Health Tips Pool - বাংলা (520+) ─────────────────────────────────
    private static final String[][] ALL_TIPS_BANGAL = {
            // পানি পান
            {"\uD83D\uDCA7", "সুস্থ থাকতে প্রতিদিন অন্তত ৮ গ্লাস পানি পান করুন।", "#1976D2"}, {"\uD83D\uDCA7", "সকালে উঠে এক গ্লাস হালকা গরম পানি হজমশক্তি বাড়ায়।", "#1976D2"}, {"\uD83D\uDCA7", "পানির বোতল সাথে রাখুন — দিনে কতটুকু খাচ্ছেন সহজে ট্র্যাক হবে।", "#1565C0"}, {"\uD83D\uDCA7", "খাওয়ার আগে পানি পান করলে ক্যালোরি গ্রহণ কমে।", "#1976D2"}, {"\uD83D\uDCA7", "ভেষজ চা দৈনিক তরল গ্রহণের মধ্যে গণনা করা যায়।", "#0288D1"}, {"\uD83D\uDCA7", "ডাবের পানি প্রাকৃতিক ইলেকট্রোলাইটের চমৎকার উৎস।", "#00796B"}, {"\uD83D\uDCA7", "তৃষ্ণাকে প্রায়ই ক্ষুধা মনে হয় — আগে পানি খান।", "#1976D2"}, {"\uD83D\uDCA7", "পানিশূন্যতায় মনোযোগ ও স্মৃতিশক্তি কমে যায়।", "#1565C0"}, {"\uD83D\uDCA7", "প্রস্রাবের রং হালকা হলুদ হওয়া মানে শরীর সঠিকভাবে হাইড্রেটেড।", "#F57C00"}, {"\uD83D\uDCA7", "ঠান্ডা পানি পান করলে বিপাকক্রিয়া সামান্য বাড়ে।", "#0288D1"}, {"\uD83D\uDCA7", "তরমুজ ও শসার মতো ফল খেলেও শরীরে পানির চাহিদা মেটে।", "#388E3C"}, {"\uD83D\uDCA7", "ব্যায়ামের ২ ঘন্টা আগে ৫০০ মিলি পানি পান করুন।", "#1976D2"}, {"\uD83D\uDCA7", "সাদা পানি একঘেয়ে লাগলে লেবু বা পুদিনাপাতা মেশান।", "#0288D1"}, {"\uD83D\uDCA7", "চা-কফি ও অ্যালকোহল শরীর থেকে পানি বের করে দেয় — বেশি পান করুন।", "#5D4037"}, {"\uD83D\uDCA7", "পর্যাপ্ত পানি মাথাব্যথা প্রতিরোধ ও উপশমে সাহায্য করে।", "#1565C0"}, {"\uD83D\uDCA7", "কিডনি সঠিকভাবে কাজ করতে পর্যাপ্ত পানির প্রয়োজন।", "#0288D1"}, {"\uD83D\uDCA7", "৬০ মিনিটের বেশি ব্যায়ামে স্পোর্টস ড্রিংক প্রয়োজন হতে পারে।", "#388E3C"}, {"\uD83D\uDCA7", "পানি হজমে সাহায্য করে ও শরীরের তরল ভারসাম্য রক্ষা করে।", "#1976D2"}, {"\uD83D\uDCA7", "নিয়মিত পানি পান ত্বককে সতেজ ও বলিরেখামুক্ত রাখে।", "#E91E63"}, {"\uD83D\uDCA7", "প্রতি ঘন্টায় পানি পানের রিমাইন্ডার সেট করুন।", "#1565C0"},

            // ঘুম
            {"\uD83D\uDE34", "সুস্বাস্থ্যের জন্য প্রতিরাতে ৭–৯ ঘন্টা ঘুম দরকার।", "#7B1FA2"}, {"\uD83D\uDE34", "সপ্তাহান্তেও একই সময়ে ঘুমান ও ওঠুন।", "#6A1B9A"}, {"\uD83D\uDE34", "ঘুমের আগে অন্তত ৩০ মিনিট মোবাইল-স্ক্রিন এড়িয়ে চলুন।", "#7B1FA2"}, {"\uD83D\uDE34", "ঘর ঠান্ডা রাখলে (১৮°C) ঘুম গভীর হয়।", "#5E35B1"}, {"\uD83D\uDE34", "বিকেল ২টার পরে ক্যাফেইন এড়িয়ে চললে রাতের ঘুম ভালো হয়।", "#4527A0"}, {"\uD83D\uDE34", "অন্ধকার ঘরে মেলাটোনিন তৈরি হয় যা ঘুম আনে।", "#7B1FA2"}, {"\uD83D\uDE34", "নিয়মিত ঘুমের রুটিন শরীরের ভেতরের ঘড়িকে ঠিক রাখে।", "#6A1B9A"}, {"\uD83D\uDE34", "২০ মিনিটের ঘুম ক্লান্তি দূর করে ঝিমুনি ছাড়াই।", "#7B1FA2"}, {"\uD83D\uDE34", "ঘুম কম হলে ক্ষুধার হরমোন (ঘ্রেলিন) বেড়ে যায়।", "#5E35B1"}, {"\uD83D\uDE34", "পর্যাপ্ত ঘুম আবেগ নিয়ন্ত্রণ ও মানসিক স্থিতিশীলতা বাড়ায়।", "#4527A0"}, {"\uD83D\uDE34", "হোয়াইট নয়েজ বা প্রকৃতির শব্দ ঘুমে সাহায্য করে।", "#7B1FA2"}, {"\uD83D\uDE34", "গভীর ঘুমে শরীরের পেশি ও টিস্যু মেরামত হয়।", "#6A1B9A"}, {"\uD83D\uDE34", "অ্যালকোহল REM ঘুম নষ্ট করে — রাতে পান করবেন না।", "#7B1FA2"}, {"\uD83D\uDE34", "ঘুমানোর আগে বই পড়া একটি চমৎকার রাতের রুটিন।", "#5E35B1"}, {"\uD83D\uDE34", "ঘুমের সময় মস্তিষ্ক স্মৃতি ও শেখা তথ্য সংরক্ষণ করে।", "#4527A0"}, {"\uD83D\uDE34", "ভালো গদিতে বিনিয়োগ করুন — জীবনের এক-তৃতীয়াংশ ঘুমে কাটে।", "#7B1FA2"}, {"\uD83D\uDE34", "ঘুমের আগে স্ট্রেচিং পেশির টান কমায় ও ঘুম ভালো করে।", "#6A1B9A"}, {"\uD83D\uDE34", "ডায়েরি লেখা উদ্বেগ কমায় ও ঘুমকে সহজ করে।", "#7B1FA2"}, {"\uD83D\uDE34", "ঘুমানোর ২–৩ ঘন্টা আগে ভারী খাবার খাবেন না।", "#5E35B1"}, {"\uD83D\uDE34", "ঘুমের অভ্যাস ডায়েট ও ব্যায়ামের মতোই গুরুত্বপূর্ণ।", "#4527A0"},

            // ব্যায়াম ও শারীরিক কার্যকলাপ
            {"\uD83D\uDEB6", "হৃদরোগ প্রতিরোধে প্রতিদিন ৭,৫০০–১০,০০০ কদম হাঁটুন।", "#388E3C"}, {"\uD83D\uDEB6", "খাওয়ার পরে ১০ মিনিট হাঁটলে রক্তে শর্করা কমে।", "#2E7D32"}, {"\uD83D\uDEB6", "সুযোগ পেলে লিফটের বদলে সিঁড়ি ব্যবহার করুন।", "#388E3C"}, {"\uD83D\uDEB6", "কাছের কাজে হেঁটে বা সাইকেল চালিয়ে যান।", "#1B5E20"}, {"\uD83D\uDEB6", "নাচ হলো মজার কার্ডিও যেটা ব্যায়ামের মতো মনে হয় না।", "#388E3C"}, {"\uD83C\uDFCB️", "সপ্তাহে ২–৩ দিন ওজন উত্তোলন হাড়ের ঘনত্ব বাড়ায়।", "#5E35B1"}, {"\uD83C\uDFCB️", "রেজিস্ট্যান্স ট্রেনিং বিশ্রামেও বিপাকক্রিয়া সক্রিয় রাখে।", "#4527A0"}, {"\uD83C\uDFCB️", "স্কোয়াট ও ডেডলিফট একসাথে অনেক পেশিতে কাজ করে।", "#5E35B1"}, {"\uD83C\uDFCB️", "ধীরে ধীরে ভার বাড়ানো নিরাপদে শক্তি তৈরি করে।", "#4527A0"}, {"\uD83C\uDFCB️", "মূল পেশি শক্তিশালী হলে মেরুদণ্ড সুরক্ষিত থাকে।", "#5E35B1"}, {"⏱️", "ডেস্কে কাজ করলে প্রতি ঘন্টায় ৫ মিনিট হাঁটুন।", "#00796B"}, {"⏱️", "দীর্ঘক্ষণ বসে থাকা হৃদরোগের ঝুঁকি বাড়ায়।", "#00695C"}, {"⏱️", "স্ট্যান্ডিং ডেস্ক ব্যবহার করুন বা বসা-দাঁড়ানো পালা করুন।", "#00796B"}, {"⏱️", "টিভি দেখার ফাঁকে স্কোয়াট বা ক্যালফ রেইজ করুন।", "#00695C"}, {"⏱️", "ব্যায়ামকে মিটিংয়ের মতো ক্যালেন্ডারে যোগ করুন।", "#00796B"}, {"\uD83E\uDDD8", "যোগব্যায়াম নমনীয়তা, শক্তি ও মানসিক শান্তি বাড়ায়।", "#0288D1"}, {"\uD83E\uDDD8", "পিলাটেস মূল পেশি শক্তিশালী ও ভঙ্গি উন্নত করে।", "#0277BD"}, {"\uD83E\uDDD8", "সাঁতার কম চাপে সম্পূর্ণ শরীরের ব্যায়াম।", "#0288D1"}, {"\uD83E\uDDD8", "সাইকেল চালানো হাঁটুর জন্য নিরাপদ কার্ডিও।", "#0277BD"}, {"\uD83E\uDDD8", "দড়িলাফ অধিকাংশ ব্যায়ামের চেয়ে বেশি ক্যালোরি পোড়ায়।", "#0288D1"}, {"\uD83C\uDFC3", "HIIT ব্যায়াম কম সময়ে বেশি চর্বি পোড়ায়।", "#C62828"}, {"\uD83C\uDFC3", "দৌড়ানো হৃদরোগ ও মানসিক স্বাস্থ্য উন্নত করে।", "#B71C1C"}, {"\uD83C\uDFC3", "ব্যায়ামের আগে ওয়ার্ম আপ করুন — চোট এড়াতে।", "#C62828"}, {"\uD83C\uDFC3", "ব্যায়ামের পরে কুল ডাউন করুন — হৃদস্পন্দন স্বাভাবিক করতে।", "#B71C1C"}, {"\uD83C\uDFC3", "বিশ্রামের দিন ব্যায়ামের দিনের মতোই গুরুত্বপূর্ণ।", "#C62828"},

            // পুষ্টি
            {"\uD83E\uDD66", "বিভিন্ন রঙের সবজি খান — বিভিন্ন পুষ্টি পাবেন।", "#F57C00"}, {"\uD83E\uDD66", "পালং শাক ও কেল পৃথিবীর সবচেয়ে পুষ্টিকর খাবারের মধ্যে।", "#388E3C"}, {"\uD83E\uDD66", "প্রতি বেলায় প্লেটের অর্ধেক সবজি দিয়ে ভরুন।", "#F57C00"}, {"\uD83E\uDD66", "ব্রকলিতে রয়েছে ভিটামিন C, আঁশ ও ক্যান্সারবিরোধী উপাদান।", "#388E3C"}, {"\uD83E\uDD66", "দই, কিমচির মতো গাঁজানো খাবার অন্ত্রের স্বাস্থ্য রক্ষা করে।", "#795548"}, {"\uD83C\uDF4E", "রোজ ফল খান — বিশেষত আপেল, যা পুষ্টিগুণে ভরপুর।", "#558B2F"}, {"\uD83C\uDF4E", "বেরিজাতীয় ফলে অ্যান্টিঅক্সিডেন্ট কোষ ক্ষতি রোধ করে।", "#7B1FA2"}, {"\uD83C\uDF4E", "আভোকাডোতে হার্টের জন্য উপকারী স্বাস্থ্যকর চর্বি আছে।", "#558B2F"}, {"\uD83C\uDF4E", "কলা পটাশিয়াম ও প্রাকৃতিক শক্তির ভালো উৎস।", "#F57C00"}, {"\uD83C\uDF4E", "লেবুজাতীয় ফলে ভিটামিন C রোগপ্রতিরোধ ক্ষমতা বাড়ায়।", "#F9A825"}, {"\uD83E\uDDC2", "রক্তচাপ ঠিক রাখতে দিনে ২,৩০০ মিগ্রার বেশি লবণ খাবেন না।", "#C62828"}, {"\uD83E\uDDC2", "প্রক্রিয়াজাত খাবারে লুকানো লবণ থাকে — লেবেল পড়ুন।", "#B71C1C"}, {"\uD83E\uDDC2", "লবণের বদলে মশলা ও ভেষজ দিয়ে রান্না করুন।", "#C62828"}, {"\uD83E\uDDC2", "পটাশিয়ামসমৃদ্ধ খাবার উচ্চ রক্তচাপের প্রভাব কমায়।", "#388E3C"}, {"\uD83D\uDC1F", "স্যামন ও ইলিশে ওমেগা-৩ ফ্যাটি এসিড ভরপুর।", "#0288D1"}, {"\uD83D\uDC1F", "ওমেগা-৩ প্রদাহ কমায় ও মস্তিষ্ক-হার্টের স্বাস্থ্য রক্ষা করে।", "#0277BD"}, {"\uD83D\uDC1F", "সপ্তাহে অন্তত ২ বার মাছ খান।", "#0288D1"}, {"\uD83D\uDC1F", "তিসির বীজ ও আখরোট উদ্ভিদভিত্তিক ওমেগা-৩-এর উৎস।", "#5E35B1"}, {"\uD83D\uDC1F", "মাছ খাওয়া বিষণ্নতা ও উদ্বেগ কমাতে সাহায্য করে।", "#0288D1"}, {"\uD83E\uDD5A", "ডিম পুষ্টিগুণে সম্পূর্ণ একটি খাবার।", "#F57C00"}, {"\uD83E\uDD5A", "ডিমে কোলিন আছে যা মস্তিষ্কের বিকাশে সহায়ক।", "#F57C00"}, {"\uD83E\uDD5A", "মসুর ডাল ও ছোলা উদ্ভিদভিত্তিক প্রোটিনের চমৎকার উৎস।", "#795548"}, {"\uD83E\uDD5A", "বাদাম ও বীজে স্বাস্থ্যকর চর্বি, প্রোটিন ও খনিজ আছে।", "#5D4037"}, {"\uD83E\uDD5A", "কিনোয়ায় নয়টি প্রয়োজনীয় অ্যামিনো অ্যাসিড আছে।", "#388E3C"}, {"\uD83C\uDF5A", "পরিশোধিত শস্যের বদলে লাল চাল ও লাল আটা বেছে নিন।", "#795548"}, {"\uD83C\uDF5A", "ওটস, যব রক্তে শর্করা স্থিতিশীল রাখে।", "#6D4C41"}, {"\uD83C\uDF5A", "আঁশসমৃদ্ধ খাবার অন্ত্রের ভালো ব্যাকটেরিয়া পোষে।", "#795548"}, {"\uD83C\uDF5A", "ওভারনাইট ওটস দ্রুত ও পুষ্টিকর সকালের নাস্তা।", "#6D4C41"}, {"\uD83C\uDF5A", "সাদা রুটি ও মিষ্টি সিরিয়াল রক্তে শর্করা হঠাৎ বাড়িয়ে দেয়।", "#795548"},

            // মানসিক স্বাস্থ্য
            {"\uD83E\uDDD8", "নিয়মিত ধ্যান মানসিক চাপ উল্লেখযোগ্যভাবে কমায়।", "#0288D1"}, {"\uD83E\uDDD8", "মাত্র ৫ মিনিটের মাইন্ডফুলনেস কর্টিসল কমায়।", "#0277BD"}, {"\uD83E\uDDD8", "কৃতজ্ঞতার ডায়েরি মেজাজ ও জীবনসন্তুষ্টি বাড়ায়।", "#0288D1"}, {"\uD83E\uDDD8", "প্রকৃতিতে সময় কাটালে উদ্বেগ কমে ও মন ভালো থাকে।", "#388E3C"}, {"\uD83E\uDDD8", "সামাজিক সম্পর্ক দীর্ঘ ও সুস্থ জীবনের সবচেয়ে বড় নিয়ামক।", "#7B1FA2"}, {"\uD83E\uDDD8", "পরোপকার করলে দাতা ও গ্রহীতা উভয়ের সেরোটোনিন বাড়ে।", "#0288D1"}, {"\uD83E\uDDD8", "হাসি আসলেই ওষুধ — মানসিক চাপের হরমোন কমায়।", "#F57C00"}, {"\uD83E\uDDD8", "সীমানা নির্ধারণ মানসিক শক্তি রক্ষা করে ও বার্নআউট প্রতিরোধ করে।", "#0277BD"}, {"\uD83E\uDDD8", "থেরাপিস্টের সাথে কথা বলা দুর্বলতা নয়, সাহসিকতা।", "#0288D1"}, {"\uD83E\uDDD8", "সপ্তাহে একদিন ডিজিটাল ডিটক্স উদ্বেগ কমায়।", "#388E3C"}, {"\uD83E\uDDD8", "আঁকা, গান — সৃজনশীল কাজ সক্রিয় ধ্যানের কাজ করে।", "#7B1FA2"}, {"\uD83E\uDDD8", "শারীরিক ব্যায়াম হালকা বিষণ্নতায় ওষুধের মতো কার্যকর।", "#C62828"}, {"\uD83E\uDDD8", "সচেতনভাবে খাওয়া অতিরিক্ত খাওয়া ও খাদ্যাপরাধবোধ কমায়।", "#0288D1"}, {"\uD83E\uDDD8", "উদ্বেগ বাড়লে সংবাদ দেখা সীমিত করুন।", "#0277BD"}, {"\uD83E\uDDD8", "দীর্ঘমেয়াদী সুস্বাস্থ্যে আত্মদয়া কঠোর শৃঙ্খলার মতোই জরুরি।", "#0288D1"},

            // সূর্য ও ভিটামিন
            {"☀️", "ভিটামিন D তৈরিতে প্রতিদিন ১৫–২০ মিনিট রোদে থাকুন।", "#FFA000"}, {"☀️", "ভিটামিন D-এর অভাবে বিষণ্নতা, ক্লান্তি ও রোগপ্রতিরোধ দুর্বল হয়।", "#FF8F00"}, {"☀️", "সকালের রোদ শরীরের ভেতরের ঘড়ি ঠিক রাখে ও ঘুম ভালো করে।", "#FFA000"}, {"☀️", "দুপুরের আগের রোদ ভিটামিন D তৈরিতে বেশি কার্যকর।", "#FF8F00"}, {"☀️", "গাঢ় ত্বকের মানুষদের বেশি সময় রোদে থাকতে হয়।", "#FFA000"}, {"\uD83E\uDDF4", "মেঘলা দিনেও UV ক্ষতি হয় — প্রতিদিন সানস্ক্রিন মাখুন।", "#E65100"}, {"\uD83E\uDDF4", "গাড়ির কাচ UV ঠেকাতে পারে না — বের হওয়ার আগে সানস্ক্রিন মাখুন।", "#BF360C"}, {"\uD83E\uDDF4", "বাইরে থাকলে প্রতি ২ ঘন্টায় সানস্ক্রিন পুনরায় মাখুন।", "#E65100"}, {"\uD83E\uDDF4", "সানগ্লাস চোখকে UV-জনিত ম্যাকুলার ক্ষতি থেকে রক্ষা করে।", "#1565C0"}, {"\uD83E\uDDF4", "সানস্ক্রিনের চেয়ে সুরক্ষামূলক পোশাক UV থেকে বেশি রক্ষা করে।", "#E65100"},

            // শ্বাস ও শিথিলায়ন
            {"\uD83E\uDEB1", "৫ মিনিট গভীর শ্বাস নিলে হৃদস্পন্দন দ্রুত কমে।", "#00838F"}, {"\uD83E\uDEB1", "বক্স ব্রিদিং (৪-৪-৪-৪) প্যারাসিমপ্যাথেটিক সিস্টেম সক্রিয় করে।", "#00695C"}, {"\uD83E\uDEB1", "পেট দিয়ে শ্বাস নেওয়া মূল পেশি সক্রিয় করে ও টান কমায়।", "#00838F"}, {"\uD83E\uDEB1", "৪-৭-৮ শ্বাস-কৌশল দ্রুত ঘুমিয়ে পড়তে সাহায্য করে।", "#00695C"}, {"\uD83E\uDEB1", "নাক দিয়ে ধীরে শ্বাস নিলে ব্যায়ামে অক্সিজেন ব্যবহার উন্নত হয়।", "#00838F"}, {"\uD83E\uDEB1", "প্রগতিশীল পেশি শিথিলায়ন শরীরে জমা মানসিক চাপ কমায়।", "#00695C"}, {"\uD83E\uDEB1", "নাসারন্ধ্র পর্যায়ক্রমে বন্ধ করে শ্বাস নিলে স্নায়ুতন্ত্র সুষম হয়।", "#00838F"}, {"\uD83E\uDEB1", "একটি সচেতন গভীর শ্বাসও মানসিক চাপ কমাতে পারে।", "#00695C"}, {"\uD83E\uDEB1", "নিয়মিত শ্বাসক্রিয়া অনুশীলন রক্তচাপ কমায়।", "#00838F"}, {"\uD83E\uDEB1", "ঘুমের মধ্যে মুখ দিয়ে শ্বাস নিলে ঘুমের মান কমে।", "#00695C"},

            // ভঙ্গি ও এরগোনমিক্স
            {"\uD83E\uDDD8", "সোজা হয়ে বসুন — কুঁজো হলে ঘাড় ও পিঠে দীর্ঘস্থায়ী ব্যথা হয়।", "#5E35B1"}, {"\uD83E\uDDD8", "মনিটর চোখের সমান উচ্চতায় রাখুন — ঘাড়ের চাপ কমবে।", "#4527A0"}, {"\uD83E\uDDD8", "এরগোনমিক চেয়ার দীর্ঘ কাজের সময় কোমর রক্ষা করে।", "#5E35B1"}, {"\uD83E\uDDD8", "ফোন ব্যবহারে কাঁধ পিছনে টানুন — কুঁজো হওয়া ঠেকাতে।", "#4527A0"}, {"\uD83E\uDDD8", "মূল পেশি শক্তিশালী করা পিঠ ব্যথার সেরা দীর্ঘমেয়াদী সমাধান।", "#5E35B1"}, {"\uD83E\uDDD8", "পাশ ফিরে ঘুমালে পিঠ ব্যথা কমে ও মেরুদণ্ড সুরক্ষিত থাকে।", "#4527A0"}, {"\uD83E\uDDD8", "হাঁটুর মাঝে বালিশ রেখে ঘুমালে কোমর ও নিতম্বের চাপ কমে।", "#5E35B1"}, {"\uD83E\uDDD8", "হিপ ফ্লেক্সর স্ট্রেচিং সারাদিন বসার ক্ষতি পুষিয়ে দেয়।", "#4527A0"}, {"\uD83E\uDDD8", "ফোন চোখের সামনে ধরুন — ঘাড় ঝুঁকিয়ে রাখবেন না।", "#5E35B1"}, {"\uD83E\uDDD8", "১০ মিনিট ফোম রোলিং পেশির ক্লান্তি ও ব্যথা কমায়।", "#4527A0"},

            // হার্টের স্বাস্থ্য
            {"\u2764\uFE0F", "হৃদরোগ বিশ্বে মৃত্যুর প্রধান কারণ — আজই সতর্ক হন।", "#C62828"}, {"\u2764\uFE0F", "নিয়মিত এরোবিক ব্যায়াম হার্টের পেশি সরাসরি শক্তিশালী করে।", "#B71C1C"}, {"\u2764\uFE0F", "ভূমধ্যসাগরীয় খাদ্যাভ্যাস হৃদরোগের ঝুঁকি উল্লেখযোগ্যভাবে কমায়।", "#C62828"}, {"\u2764\uFE0F", "ট্রান্স চর্বি খারাপ কোলেস্টেরল বাড়ায় — এড়িয়ে চলুন।", "#B71C1C"}, {"\u2764\uFE0F", "ধূমপান হৃদরোগের সবচেয়ে বড় প্রতিরোধযোগ্য কারণ।", "#C62828"}, {"\u2764\uFE0F", "উচ্চ রক্তচাপ 'নীরব ঘাতক' — নিয়মিত পরীক্ষা করুন।", "#B71C1C"}, {"\u2764\uFE0F", "ডার্ক চকোলেটে (৭০%+) ফ্ল্যাভোনয়েড হার্টের উপকার করে।", "#5D4037"}, {"\u2764\uFE0F", "হাসি ও ইতিবাচক আবেগ হৃদরোগের ঝুঁকির নির্দেশক কমায়।", "#C62828"}, {"\u2764\uFE0F", "দীর্ঘমেয়াদী মানসিক চাপ নিয়ন্ত্রণ হার্টের সুরক্ষার জন্য জরুরি।", "#B71C1C"}, {"\u2764\uFE0F", "পর্যাপ্ত ঘুম হার্ট অ্যাটাক ও স্ট্রোকের ঝুঁকি কমায়।", "#7B1FA2"},

            // অন্ত্রের স্বাস্থ্য
            {"\uD83E\uDDA0", "আপনার অন্ত্রে কোটি কোটি ব্যাকটেরিয়া স্বাস্থ্য নিয়ন্ত্রণ করে।", "#795548"}, {"\uD83E\uDDA0", "দই, কেফির ও সাউরক্রট অন্ত্রের ভালো ব্যাকটেরিয়া বাড়ায়।", "#6D4C41"}, {"\uD83E\uDDA0", "রসুন, পেঁয়াজ ও কলা ভালো ব্যাকটেরিয়ার খাবার।", "#795548"}, {"\uD83E\uDDA0", "অ্যান্টিবায়োটিকের পরে প্রোবায়োটিক খান — অন্ত্র পুনরুদ্ধারে।", "#6D4C41"}, {"\uD83E\uDDA0", "বৈচিত্র্যময় খাদ্যাভ্যাস অন্ত্রের মাইক্রোবায়োমকে শক্তিশালী করে।", "#795548"}, {"\uD83E\uDDA0", "দীর্ঘস্থায়ী মানসিক চাপ অন্ত্র-মস্তিষ্কের সংযোগ ক্ষতিগ্রস্ত করে।", "#6D4C41"}, {"\uD83E\uDDA0", "ভালোভাবে চিবিয়ে খান — হজম শুরু হয় মুখেই।", "#795548"}, {"\uD83E\uDDA0", "সুস্থ অন্ত্র আপনার রোগপ্রতিরোধ ব্যবস্থার ৭০% নিয়ন্ত্রণ করে।", "#6D4C41"}, {"\uD83E\uDDA0", "কৃত্রিম মিষ্টি অন্ত্রের মাইক্রোবায়োমে বিরূপ প্রভাব ফেলতে পারে।", "#795548"}, {"\uD83E\uDDA0", "ধীরে খাওয়া হজম ও তৃপ্তি দুটোই উন্নত করে।", "#6D4C41"},

            // চোখের স্বাস্থ্য
            {"\uD83D\uDC41\uFE0F", "২০-২০-২০ নিয়ম: ২০ মিনিটে একবার ২০ ফুট দূরে ২০ সেকেন্ড তাকান।", "#0288D1"}, {"\uD83D\uDC41\uFE0F", "স্ক্রিনের উজ্জ্বলতা কমান ও রাতের মোড ব্যবহার করুন।", "#0277BD"}, {"\uD83D\uDC41\uFE0F", "গাজরে বিটা-ক্যারোটিন আছে যা ভিটামিন A-তে রূপান্তরিত হয়।", "#F57C00"}, {"\uD83D\uDC41\uFE0F", "সবুজ শাকসবজিতে লুটেইন রেটিনাকে ক্ষতির হাত থেকে রক্ষা করে।", "#388E3C"}, {"\uD83D\uDC41\uFE0F", "বার্ষিক চোখ পরীক্ষায় গ্লুকোমা ও ম্যাকুলার সমস্যা আগে ধরা পড়ে।", "#0288D1"}, {"\uD83D\uDC41\uFE0F", "পর্যাপ্ত ঘুম চোখের স্বাস্থ্য ও শুষ্কতা প্রতিরোধে জরুরি।", "#7B1FA2"}, {"\uD83D\uDC41\uFE0F", "সানগ্লাস চোখের লেন্সে ক্রমাগত UV ক্ষতি কমায়।", "#E65100"}, {"\uD83D\uDC41\uFE0F", "সচেতনভাবে পলক ফেলুন — ডিজিটাল চোখ ক্লান্তি কমবে।", "#0288D1"}, {"\uD83D\uDC41\uFE0F", "ব্লু-লাইট ফিল্টার চশমা চোখের ক্লান্তি কমাতে পারে।", "#0277BD"}, {"\uD83D\uDC41\uFE0F", "ক্যাফেইন চোখ শুকিয়ে দেয় — বেশি পানি পান করুন।", "#1976D2"},

            // দাঁতের স্বাস্থ্য
            {"\uD83E\uDDB7", "দাঁতের ক্ষয় রোধে দিনে দুবার ২ মিনিট ব্রাশ করুন।", "#00796B"}, {"\uD83E\uDDB7", "ফ্লস করুন প্রতিদিন — ব্রাশ দাঁতের ৪০% অংশ পরিষ্কার করতে পারে না।", "#00695C"}, {"\uD83E\uDDB7", "মাউথওয়াশ মুখের দুর্গন্ধ ও মাড়ির রোগের ব্যাকটেরিয়া মারে।", "#00796B"}, {"\uD83E\uDDB7", "মুখের স্বাস্থ্য খারাপ থাকলে হৃদরোগ ও ডায়াবেটিসের ঝুঁকি বাড়ে।", "#C62828"}, {"\uD83E\uDDB7", "মিষ্টি পানীয় কঠিন মিষ্টি খাবারের চেয়ে দাঁতের বেশি ক্ষতি করে।", "#00796B"}, {"\uD83E\uDDB7", "ব্যথা না থাকলেও ৬ মাসে একবার দাঁতের ডাক্তার দেখান।", "#00695C"}, {"\uD83E\uDDB7", "চিনিমুক্ত চুইংগাম লালা তৈরি করে যা দাঁত রক্ষা করে।", "#00796B"}, {"\uD83E\uDDB7", "ইলেকট্রিক টুথব্রাশ সাধারণের চেয়ে অনেক বেশি কার্যকর।", "#00695C"}, {"\uD83E\uDDB7", "ক্যালসিয়াম ও ভিটামিন D দাঁতের এনামেল মজবুত করে।", "#00796B"}, {"\uD83E\uDDB7", "জিহ্বা পরিষ্কার করলে দুর্গন্ধ সৃষ্টিকারী ব্যাকটেরিয়া কমে।", "#00695C"},

            // ত্বকের স্বাস্থ্য
            {"\uD83C\uDF3F", "প্রতিদিন ময়েশ্চারাইজার মাখুন — ত্বকের আর্দ্রতা রক্ষা করতে।", "#E91E63"}, {"\uD83C\uDF3F", "ভিটামিন C সিরাম ত্বক উজ্জ্বল করে ও কোলাজেন তৈরিতে সাহায্য করে।", "#F57C00"}, {"\uD83C\uDF3F", "রেটিনল বার্ধক্যবিরোধী সবচেয়ে প্রমাণিত উপাদানগুলোর একটি।", "#E91E63"}, {"\uD83C\uDF3F", "মানসিক চাপের হরমোন কর্টিসল ব্রণ ও ত্বকের প্রদাহ বাড়ায়।", "#C62828"}, {"\uD83C\uDF3F", "অতিরিক্ত চিনি গ্লাইকেশনের মাধ্যমে ত্বক বুড়িয়ে দেয়।", "#795548"}, {"\uD83C\uDF3F", "রাতে ঘুমানোর আগে মেকআপ তুলুন — ছিদ্র বন্ধ হবে না।", "#E91E63"}, {"\uD83C\uDF3F", "ঠান্ডা পানির ঝরনা ছিদ্র সংকুচিত করে ও রক্তসঞ্চালন বাড়ায়।", "#1976D2"}, {"\uD83C\uDF3F", "ওমেগা-৩ ফ্যাটি অ্যাসিড ত্বকের প্রদাহ ও শুষ্কতা কমায়।", "#0288D1"}, {"\uD83C\uDF3F", "অতিরিক্ত গরম পানিতে গোসল ত্বকের প্রাকৃতিক তেল নষ্ট করে।", "#E65100"}, {"\uD83C\uDF3F", "হায়ালুরোনিক অ্যাসিড নিজের ওজনের ১,০০০ গুণ পানি ধরে রাখে।", "#E91E63"},

            // রোগপ্রতিরোধ ব্যবস্থা
            {"\uD83D\uDEE1\uFE0F", "সুষম খাদ্যাভ্যাস শক্তিশালী রোগপ্রতিরোধ ব্যবস্থার ভিত্তি।", "#388E3C"}, {"\uD83D\uDEE1\uFE0F", "ভিটামিন C, D ও জিংক রোগপ্রতিরোধ ক্ষমতার জন্য অপরিহার্য।", "#F57C00"}, {"\uD83D\uDEE1\uFE0F", "নিয়মিত মাঝারি ব্যায়াম রোগপ্রতিরোধ ক্ষমতা উল্লেখযোগ্যভাবে বাড়ায়।", "#388E3C"}, {"\uD83D\uDEE1\uFE0F", "দীর্ঘস্থায়ী মানসিক চাপ ধীরে ধীরে রোগপ্রতিরোধ ব্যবস্থা দুর্বল করে।", "#C62828"}, {"\uD83D\uDEE1\uFE0F", "৭–৯ ঘন্টার ঘুম সবচেয়ে ভালো রোগপ্রতিরোধ বুস্টার।", "#7B1FA2"}, {"\uD83D\uDEE1\uFE0F", "অন্ত্রের স্বাস্থ্য ও রোগপ্রতিরোধ ব্যবস্থা গভীরভাবে সংযুক্ত।", "#795548"}, {"\uD83D\uDEE1\uFE0F", "অতিরিক্ত অ্যালকোহল রোগপ্রতিরোধ ক্ষমতা ও সুস্থতা কমায়।", "#C62828"}, {"\uD83D\uDEE1\uFE0F", "টিকা রোগপ্রতিরোধ ক্ষমতা বাড়ানোর সবচেয়ে কার্যকর উপায়।", "#388E3C"}, {"\uD83D\uDEE1\uFE0F", "রসুনে প্রাকৃতিক অ্যান্টিমাইক্রোবিয়াল ও রোগপ্রতিরোধ-বর্ধক উপাদান আছে।", "#F57C00"}, {"\uD83D\uDEE1\uFE0F", "এলডারবেরি নির্যাস সর্দি-কাশির স্থায়িত্ব কমাতে পারে।", "#7B1FA2"},

            // ওজন ব্যবস্থাপনা
            {"\u2696\uFE0F", "টেকসই ওজন কমানো সপ্তাহে ০.৫–১ কেজি — ধীরে কিন্তু স্থায়ীভাবে।", "#795548"}, {"\u2696\uFE0F", "নির্দিষ্ট ডায়েট নয়, ক্যালোরি ঘাটতিই ওজন কমানোর মূল চাবিকাঠি।", "#6D4C41"}, {"\u2696\uFE0F", "প্রতি বেলায় প্রোটিন খেলে ক্ষুধা কমে ও পেশি রক্ষা হয়।", "#795548"}, {"\u2696\uFE0F", "জুস ও কোমল পানীয়ের তরল ক্যালোরি সহজেই অগোচরে থেকে যায়।", "#C62828"}, {"\u2696\uFE0F", "সচেতনভাবে খাওয়া অচেতন অতিরিক্ত খাওয়া ও তৃপ্তিহীনতা প্রতিরোধ করে।", "#795548"}, {"\u2696\uFE0F", "ঘুম কম হলে উচ্চ-ক্যালোরি জাঙ্কফুডের লালসা বাড়ে।", "#7B1FA2"}, {"\u2696\uFE0F", "শক্তি প্রশিক্ষণ ডায়েটিংয়ে পেশির ক্ষয় রোধ করে।", "#5E35B1"}, {"\u2696\uFE0F", "বাড়িতে রান্না করলে উপাদান ও পরিমাণে পূর্ণ নিয়ন্ত্রণ থাকে।", "#388E3C"}, {"\u2696\uFE0F", "ধীরে খেলে মস্তিষ্ক পেটভর্তির সংকেত পেতে পারে।", "#795548"}, {"\u2696\uFE0F", "স্বাস্থ্যকর অভ্যাস তৈরিতে মনোযোগ দিন — ওজন এমনিতেই কমবে।", "#6D4C41"},

            // ডায়াবেটিস ও রক্তে শর্করা
            {"\uD83E\uDE78", "প্রোটিন ও চর্বির সাথে শর্করা খেলে রক্তে শর্করার হঠাৎ বৃদ্ধি কমে।", "#C62828"}, {"\uD83E\uDE78", "খাওয়ার পরে হাঁটা রক্তে শর্করা নিয়ন্ত্রণের সবচেয়ে কার্যকর উপায়।", "#388E3C"}, {"\uD83E\uDE78", "দারুচিনি ইনসুলিন সংবেদনশীলতা উন্নত করতে পারে।", "#795548"}, {"\uD83E\uDE78", "পরিশোধিত শর্করা রক্তে শর্করা দ্রুত বাড়িয়ে কমিয়ে দেয়।", "#C62828"}, {"\uD83E\uDE78", "খাওয়ার আগে আপেল সাইডার ভিনেগার রক্তে শর্করার প্রতিক্রিয়া কমাতে পারে।", "#795548"}, {"\uD83E\uDE78", "নিয়মিত খালি পেটের রক্তে শর্করা পরীক্ষায় প্রি-ডায়াবেটিস আগে ধরা পড়ে।", "#C62828"}, {"\uD83E\uDE78", "টাইপ-২ ডায়াবেটিস মূলত জীবনযাত্রার পরিবর্তনে প্রতিরোধযোগ্য।", "#388E3C"}, {"\uD83E\uDE78", "উচ্চ আঁশের খাবার গ্লুকোজ শোষণ ধীর করে ও শক্তি স্থিতিশীল রাখে।", "#795548"}, {"\uD83E\uDE78", "মানসিক চাপ কর্টিসল বাড়ায় যা রক্তে শর্করা বৃদ্ধি করে।", "#C62828"}, {"\uD83E\uDE78", "বিরতিহীন উপবাস সময়ের সাথে সাথে ইনসুলিন সংবেদনশীলতা উন্নত করতে পারে।", "#0288D1"},

            // শক্তি ও হরমোন
            {"\u26A1", "দীর্ঘস্থায়ী ক্লান্তি থাইরয়েড, আয়রন বা ভিটামিন B12-এর অভাব জানাতে পারে।", "#F57C00"}, {"\u26A1", "B ভিটামিন প্রতিটি কোষে শক্তি বিপাকের জন্য অপরিহার্য।", "#FF8F00"}, {"\u26A1", "আয়রনের ঘাটতিজনিত রক্তাল্পতা বিশ্বের সবচেয়ে সাধারণ পুষ্টিঘাটতি।", "#C62828"}, {"\u26A1", "ম্যাগনেসিয়াম ৩০০-রও বেশি এনজাইম বিক্রিয়ায় শক্তি জোগায়।", "#388E3C"}, {"\u26A1", "থাইরয়েডের সুস্বাস্থ্যের জন্য আয়োডিন ও সেলেনিয়াম দরকার।", "#F57C00"}, {"\u26A1", "কর্টিসল ভারসাম্য রাখতে পর্যাপ্ত ঘুম ও মানসিক চাপ নিয়ন্ত্রণ জরুরি।", "#7B1FA2"}, {"\u26A1", "টেস্টোস্টেরন ও ইস্ট্রোজেন ঘুম, খাদ্য ও ব্যায়াম দ্বারা প্রভাবিত হয়।", "#5E35B1"}, {"\u26A1", "খুব কম খেলে বিপাকক্রিয়া কমে ও কর্টিসল বেড়ে যায়।", "#C62828"}, {"\u26A1", "অশ্বগন্ধার মতো অ্যাডাপ্টোজেনিক ভেষজ হরমোনের ভারসাম্য সাহায্য করতে পারে।", "#795548"}, {"\u26A1", "নিয়মিত রক্ত পরীক্ষায় হরমোনাল ও পুষ্টির স্বাস্থ্য ট্র্যাক করা যায়।", "#0288D1"},

            // শিশু ও পারিবারিক স্বাস্থ্য
            {"\uD83D\uDC76", "শিশুদের প্রতিদিন ৬০ মিনিট মাঝারি থেকে জোরালো খেলাধুলা দরকার।", "#388E3C"}, {"\uD83D\uDC76", "৬ বছরের নিচে শিশুদের স্ক্রিন টাইম দিনে ১ ঘন্টার মধ্যে রাখুন।", "#F57C00"}, {"\uD83D\uDC76", "৬ মাস বুকের দুধ খাওয়ানো শিশুর রোগপ্রতিরোধে অপরিহার্য সুরক্ষা দেয়।", "#E91E63"}, {"\uD83D\uDC76", "পারিবারিক খাবারে সবজি রাখলে শিশু ছোটবেলা থেকে সবজি পছন্দ করে।", "#388E3C"}, {"\uD83D\uDC76", "বাবা-মা যা করেন, শিশু তা শেখে — সুস্বাস্থ্যের অনুকরণ করুন।", "#F57C00"}, {"\uD83D\uDC76", "শৈশবের স্থূলতা প্রাপ্তবয়স্কে দীর্ঘস্থায়ী রোগের ঝুঁকি বহুগুণ বাড়ায়।", "#C62828"}, {"\uD83D\uDC76", "ঘুমানোর আগে বই পড়া শিশুর জন্য সেরা রাতের রুটিন।", "#7B1FA2"}, {"\uD83D\uDC76", "বাইরে খেলা হাড়ের বিকাশ ও ভিটামিন D সংশ্লেষণে সাহায্য করে।", "#388E3C"}, {"\uD83D\uDC76", "নির্ধারিত সময়সূচিতে শিশুর সব টিকা নিশ্চিত করুন।", "#0288D1"}, {"\uD83D\uDC76", "পারিবারিক খাবার শিশুর মানসিক স্বাস্থ্যের সাথে ইতিবাচকভাবে যুক্ত।", "#795548"},

            // বার্ধক্যে সুস্থ থাকা
            {"\uD83E\uDDD3", "৩০ বছরের পর থেকে পেশি বছরে ১–২% কমে — শক্তি প্রশিক্ষণ করুন।", "#5E35B1"}, {"\uD83E\uDDD3", "ভারসাম্য ব্যায়াম বৃদ্ধ বয়সে পড়ে যাওয়ার ঝুঁকি কমায়।", "#4527A0"}, {"\uD83E\uDDD3", "বৃদ্ধ বয়সে একাকীত্ব ডিমেনশিয়ার ঝুঁকি উল্লেখযোগ্যভাবে বাড়ায়।", "#7B1FA2"}, {"\uD83E\uDDD3", "ধাঁধা ও মানসিক চ্যালেঞ্জ মস্তিষ্কের নমনীয়তা রক্ষা করে।", "#5E35B1"}, {"\uD83E\uDDD3", "আজীবন শেখার অভ্যাসীরা দীর্ঘ ও সুস্থ জীবন যাপন করেন।", "#4527A0"}, {"\uD83E\uDDD3", "ক্যালোরি সীমাবদ্ধতা ও বিরতিহীন উপবাস দীর্ঘায়ুর সাথে সম্পর্কিত।", "#0288D1"}, {"\uD83E\uDDD3", "জীবনের উদ্দেশ্যবোধ সুস্থ দীর্ঘায়ু বাড়ায়।", "#7B1FA2"}, {"\uD83E\uDDD3", "হাড়ের ঘনত্ব ৩০ বছরে সর্বোচ্চ হয় — ভার বহনকারী ব্যায়াম তা রক্ষা করে।", "#5E35B1"}, {"\uD83E\uDDD3", "শ্রবণশক্তি হ্রাস চিকিৎসা না হলে জ্ঞানীয় অবনতির সাথে যুক্ত।", "#4527A0"}, {"\uD83E\uDDD3", "শক্তিশালী সামাজিক নেটওয়ার্ক সুস্থ বার্ধক্যের সেরা নিয়ামক।", "#7B1FA2"},

            // প্রতিরোধ ও নিয়মিত পরীক্ষা
            {"\uD83C\uDFE5", "বার্ষিক স্বাস্থ্য পরীক্ষায় নীরব রোগ আগে ধরা পড়ে।", "#0288D1"}, {"\uD83C\uDFE5", "১৮ বছরের পরে বছরে অন্তত একবার রক্তচাপ পরীক্ষা করুন।", "#C62828"}, {"\uD83C\uDFE5", "প্রতি ৪–৬ বছরে কোলেস্টেরল পরীক্ষা করার পরামর্শ দেওয়া হয়।", "#F57C00"}, {"\uD83C\uDFE5", "৪৫ বছর থেকে কোলোরেক্টাল ক্যান্সারের স্ক্রিনিং শুরু করুন।", "#795548"}, {"\uD83C\uDFE5", "ত্বকের ক্যান্সার সবচেয়ে সাধারণ — নিজে নিজে ত্বক পরীক্ষা করুন।", "#E65100"}, {"\uD83C\uDFE5", "স্তন ক্যান্সার স্ক্রিনিং ম্যামোগ্রাম ৪০–৫০ বছর থেকে শুরু করুন।", "#E91E63"}, {"\uD83C\uDFE5", "HPV টিকা ও প্যাপ স্মিয়ারে জরায়ুমুখের ক্যান্সার প্রতিরোধ করা যায়।", "#E91E63"}, {"\uD83C\uDFE5", "বছরে দুবার দাঁতের ডাক্তার দেখানো ব্যয়বহুল সমস্যা প্রতিরোধ করে।", "#00796B"}, {"\uD83C\uDFE5", "বয়স অনুযায়ী প্রতি ১–২ বছরে চোখ পরীক্ষা করান।", "#0288D1"}, {"\uD83C\uDFE5", "পারিবারিক স্বাস্থ্য ইতিহাস জানুন — এটি ঝুঁকির শক্তিশালী নির্দেশক।", "#795548"},

            // অ্যালকোহল ও ধূমপান
            {"\uD83D\uDEAD", "অতিরিক্ত মদ্যপান লিভার, মস্তিষ্ক, হার্ট ও রোগপ্রতিরোধ ব্যবস্থার ক্ষতি করে।", "#C62828"}, {"\uD83D\uDEAD", "মাঝারি মাত্রার মদ্যপানও কিছু ক্যান্সারের ঝুঁকি বাড়ায়।", "#B71C1C"}, {"\uD83D\uDEAD", "অ্যালকোহল ঘুমের গঠন নষ্ট করে — গভীর ঘুম কমিয়ে দেয়।", "#7B1FA2"}, {"\uD83D\uDEAD", "নন-অ্যালকোহলিক পানীয় ও মকটেল সামাজিক অনুষ্ঠানে বিকল্প হতে পারে।", "#388E3C"}, {"\uD83D\uDEAD", "অ্যালকোহল বিশ্বে প্রতিরোধযোগ্য মৃত্যুর অন্যতম প্রধান কারণ।", "#C62828"}, {"\uD83D\uDEAC", "ধূমপান ছাড়লে জীবন দীর্ঘ হয় — কখনো দেরি হয় না।", "#C62828"}, {"\uD83D\uDEAC", "ধূমপান বন্ধের ২০ মিনিটের মধ্যে রক্তচাপ কমতে শুরু করে।", "#B71C1C"}, {"\uD83D\uDEAC", "১ বছর ধূমপানমুক্ত থাকলে হৃদরোগের ঝুঁকি অর্ধেক হয়।", "#C62828"}, {"\uD83D\uDEAC", "ভ্যাপিং সিগারেটের নিরাপদ বিকল্প নয় — এর নিজস্ব ঝুঁকি আছে।", "#B71C1C"}, {"\uD83D\uDEAC", "নিকোটিন প্রতিস্থাপন থেরাপি ধূমপান ছাড়ার সম্ভাবনা দ্বিগুণ করে।", "#C62828"},

            // খাদ্যাভ্যাস
            {"\uD83C\uDF7D\uFE0F", "রবিবারে মিলপ্রেপ করুন — সময় বাঁচবে ও স্বাস্থ্যকর খাওয়া হবে।", "#388E3C"}, {"\uD83C\uDF7D\uFE0F", "সকালের নাস্তা কখনো বাদ দেবেন না — মস্তিষ্ক ও শরীর চাঙ্গা থাকবে।", "#F57C00"}, {"\uD83C\uDF7D\uFE0F", "স্ক্রিন ছাড়া টেবিলে বসে খেলে সচেতন খাওয়া উন্নত হয়।", "#795548"}, {"\uD83C\uDF7D\uFE0F", "অলিভ অয়েলে রান্না করুন — হার্টের জন্য উপকারী।", "#F57C00"}, {"\uD83C\uDF7D\uFE0F", "রাতে দেরিতে খাবেন না — ঘুমের সময় হজমশক্তি কমে যায়।", "#5E35B1"}, {"\uD83C\uDF7D\uFE0F", "দশকের পর দশকে খাবারের পরিমাণ দ্বিগুণ হয়েছে — ছোট প্লেট ব্যবহার করুন।", "#795548"}, {"\uD83C\uDF7D\uFE0F", "খাদ্যের লেবেল পড়লে সচেতন পুষ্টিগত সিদ্ধান্ত নেওয়া সহজ হয়।", "#0288D1"}, {"\uD83C\uDF7D\uFE0F", "মহিলাদের জন্য অতিরিক্ত চিনি দিনে ২৫ গ্রামের (৬ চা চামচ) মধ্যে রাখুন।", "#C62828"}, {"\uD83C\uDF7D\uFE0F", "পুরুষদের জন্য অতিরিক্ত চিনি দিনে ৩৬ গ্রামের (৯ চা চামচ) মধ্যে রাখুন।", "#C62828"}, {"\uD83C\uDF7D\uFE0F", "রঙিন খাবার খান — বিভিন্ন রং মানে বিভিন্ন ফাইটোনিউট্রিয়েন্ট।", "#F57C00"},

            // কাজ ও উৎপাদনশীলতা স্বাস্থ্য
            {"\uD83D\uDCBB", "এরগোনমিক কীবোর্ড ও মাউস পজিশন পুনরাবৃত্তিমূলক আঘাত প্রতিরোধ করে।", "#0288D1"}, {"\uD83D\uDCBB", "পোমোডোরো পদ্ধতি (২৫ মিনিট কাজ + ৫ মিনিট বিরতি) মনোযোগ বাড়ায়।", "#F57C00"}, {"\uD83D\uDCBB", "কাজের জায়গা গুছানো মানসিক চাপ ও জ্ঞানীয় ভার কমায়।", "#795548"}, {"\uD83D\uDCBB", "অতিরিক্ত কাজ বার্নআউট ডেকে আনে — টেকসই উৎপাদনশীলতায় বিশ্রাম দরকার।", "#C62828"}, {"\uD83D\uDCBB", "বাড়ি থেকে কাজ করলে কাজ শুরু ও শেষের সময় স্পষ্টভাবে নির্ধারণ করুন।", "#388E3C"}, {"\uD83D\uDCBB", "কাজের জায়গায় প্রাকৃতিক আলো মেজাজ, মনোযোগ ও শক্তি বাড়ায়।", "#FFA000"}, {"\uD83D\uDCBB", "দাঁড়িয়ে মিটিং ছোট হয় ও দীর্ঘক্ষণ বসা এড়ানো যায়।", "#00796B"}, {"\uD83D\uDCBB", "প্রতি ৬০ মিনিটে মাইক্রো-ব্রেক মনোযোগ পুনরুদ্ধার ও ক্লান্তি কমায়।", "#0288D1"}, {"\uD83D\uDCBB", "কাজের জায়গায় গাছ বায়ু বিশুদ্ধ করে ও মানসিক সুস্থতা বাড়ায়।", "#388E3C"}, {"\uD83D\uDCBB", "মাল্টিটাস্কিং দক্ষতা কমায় — একটি কাজে মনোযোগ দিন।", "#795548"},

            // পরিবেশগত স্বাস্থ্য
            {"\uD83C\uDF31", "ঘরের বাতাস বাইরের চেয়ে ২–৫ গুণ বেশি দূষিত হতে পারে — বায়ু চলাচল করান।", "#388E3C"}, {"\uD83C\uDF31", "স্পাইডার প্ল্যান্টের মতো গৃহস্থালী গাছ ঘরের বায়ু দূষণ পরিষ্কার করে।", "#2E7D32"}, {"\uD83C\uDF31", "প্লাস্টিকের ব্যবহার কমান — BPA ও ফথালেট হরমোন বিঘ্নিত করে।", "#F57C00"}, {"\uD83C\uDF31", "নন-স্টিক কুকওয়্যার উচ্চ তাপে বিষাক্ত পদার্থ ছাড়তে পারে।", "#C62828"}, {"\uD83C\uDF31", "ফিল্টার করা পানি ক্লোরিন, ভারী ধাতু ও মাইক্রোপ্লাস্টিক সরায়।", "#1976D2"}, {"\uD83C\uDF31", "জৈব ফল-সবজি পাতলা খোসার কীটনাশকের এক্সপোজার কমায়।", "#388E3C"}, {"\uD83C\uDF31", "শব্দ দূষণ কর্টিসল ও রক্তচাপ বাড়ায় — এক্সপোজার কমান।", "#795548"}, {"\uD83C\uDF31", "সপ্তাহে ১২০+ মিনিট প্রকৃতিতে সময় কাটানো সুস্বাস্থ্যের সাথে যুক্ত।", "#388E3C"}, {"\uD83C\uDF31", "বনস্নান (শিনরিন-ইয়োকু) মানসিক চাপের হরমোন উল্লেখযোগ্যভাবে কমায়।", "#2E7D32"}, {"\uD83C\uDF31", "খালি পায়ে ঘাসে হাঁটা প্রদাহ কমাতে পারে।", "#388E3C"},

            // অতিরিক্ত সুস্থতার টিপস
            {"\uD83D\uDCA1", "স্বাস্থ্য শুধু রোগের অনুপস্থিতি নয় — এটি সামগ্রিক সুস্থতা।", "#0288D1"}, {"\uD83D\uDCA1", "ছোট ছোট ধারাবাহিক অভ্যাস দীর্ঘমেয়াদে বড় স্বাস্থ্য পরিবর্তন আনে।", "#388E3C"}, {"\uD83D\uDCA1", "মাসিক স্বাস্থ্য পরিসংখ্যান ট্র্যাক করুন — পরিবর্তন সচেতন থাকুন।", "#0288D1"}, {"\uD83D\uDCA1", "প্রতিরোধ আর্থিক ও শারীরিকভাবে চিকিৎসার চেয়ে সস্তা।", "#C62828"}, {"\uD83D\uDCA1", "সঠিক পরিবেশ দিলে শরীরের নিজে সুস্থ হওয়ার অসাধারণ ক্ষমতা আছে।", "#388E3C"}, {"\uD83D\uDCA1", "জিনতত্ত্ব বন্দুক ভরে রাখে, কিন্তু জীবনযাত্রা ট্রিগার টানে।", "#795548"}, {"\uD83D\uDCA1", "স্বাস্থ্যে বিনিয়োগ করুন — এটি সারাজীবনে সর্বোচ্চ রিটার্ন দেয়।", "#0288D1"}, {"\uD83D\uDCA1", "স্বাস্থ্য কোনো গন্তব্য নয় — এটি প্রতিদিনের অনুশীলন।", "#388E3C"}, {"\uD83D\uDCA1", "সুঅভ্যাস শুরুর সেরা সময় ছিল গতকাল, পরের সেরা সময় এখনই।", "#F57C00"}, {"\uD83D\uDCA1", "ধারাবাহিকতা নিখুঁততার চেয়ে গুরুত্বপূর্ণ — চেষ্টা চালিয়ে যাওয়াই আসল।", "#0288D1"},

            // সুপারফুড ও পানীয়
            {"\uD83E\uDD51", "টমেটোর সাথে আভোকাডো খেলে লাইকোপেন শোষণ বাড়ে।", "#558B2F"}, {"\uD83E\uDD51", "হলুদের কার্কিউমিন কালো মরিচের সাথে খেলে আরও ভালো শোষিত হয়।", "#F57C00"}, {"\uD83E\uDD51", "সবুজ চায়ে L-থেনিন আছে যা শান্ত মনোযোগ দেয়।", "#388E3C"}, {"\uD83E\uDD51", "জাফরান হালকা বিষণ্নতার লক্ষণ কমাতে পারে বলে গবেষণায় দেখা গেছে।", "#FFA000"}, {"\uD83E\uDD51", "আদায় প্রদাহ-বিরোধী ও বমি-বিরোধী গুণ আছে।", "#795548"}, {"\uD83E\uDD51", "ব্লুবেরি স্মৃতিশক্তি উন্নত করে ও মস্তিষ্কের বার্ধক্য ধীর করে।", "#7B1FA2"}, {"\uD83E\uDD51", "আখরোট ওমেগা-৩-এর কারণে মস্তিষ্কের জন্য সেরা বাদাম।", "#5D4037"}, {"\uD83E\uDD51", "কেল পৃথিবীর সবচেয়ে পুষ্টিগুণসম্পন্ন খাবারের একটি।", "#388E3C"}, {"\uD83E\uDD51", "চিয়া বীজে আঁশ, প্রোটিন ও ওমেগা-৩ একসাথে পাওয়া যায়।", "#795548"}, {"\uD83E\uDD51", "এক্সট্রা ভার্জিন অলিভ অয়েল সবচেয়ে গবেষণা-সমর্থিত স্বাস্থ্যকর তেলের একটি।", "#F57C00"},

            // বিভিন্ন ব্যায়াম ও খেলাধুলা
            {"\uD83C\uDFCA", "সাঁতার কম চাপে সম্পূর্ণ শরীরের ব্যায়াম দেয়।", "#0288D1"}, {"\uD83C\uDFCA", "তাই চি ভারসাম্য, নমনীয়তা ও মানসিক শান্তির জন্য উপযুক্ত।", "#0277BD"}, {"\uD83C\uDFCA", "রক ক্লাইম্বিং শক্তি, সমস্যা সমাধান ও পূর্ণ শরীরের ব্যায়াম।", "#795548"}, {"\uD83C\uDFCA", "রোয়িং কম চাপে পূর্ণ শরীরের অন্যতম সেরা কার্ডিও।", "#C62828"}, {"\uD83C\uDFCA", "গ্রুপ ফিটনেস ক্লাস সামাজিক দায়বদ্ধতায় ব্যায়াম ধরে রাখতে সাহায্য করে।", "#388E3C"}, {"\uD83C\uDFCA", "খালি পায়ে প্রাকৃতিক তলায় হাঁটা পায়ের পেশি শক্তিশালী করে।", "#795548"}, {"\uD83C\uDFCA", "নমনীয়তা প্রশিক্ষণ আঘাতের ঝুঁকি কমায় ও দৈনন্দিন গতি উন্নত করে।", "#5E35B1"}, {"\uD83C\uDFCA", "অ্যাজিলিটি ট্রেনিং বয়সের সাথে কমে যাওয়া দ্রুত-সংকোচী পেশি রক্ষা করে।", "#C62828"}, {"\uD83C\uDFCA", "ক্রস-ট্রেনিং অতিরিক্ত ব্যবহারের আঘাত প্রতিরোধ করে ও সুষম ফিটনেস তৈরি করে।", "#0288D1"}, {"\uD83C\uDFCA", "ব্যায়াম বিজ্ঞানের আবিষ্কৃত সবচেয়ে কার্যকর ওষুধের সবচেয়ে কাছাকাছি।", "#388E3C"},};

    // ── Active 5 Tips (random each session) ─────────────────────────────────
    private String[][] activeTips = new String[5][];

    private int currentTipIndex = 0;
    private final Handler tipHandler = new Handler(Looper.getMainLooper());
    private static final long TIP_INTERVAL_MS = 4000L;

    private CardView cardHealthTip;
    private TextView tvTipIcon;
    TextView tvTipTitle;
    private TextView tvTipText;
    private LinearLayout tipDotContainer;

    // ── Step/Calorie real-time update ────────────────────────────────────────
    private TextView tvStepsValue;
    private TextView tvStepsGoal;
    private TextView tvCalValue;

    private final BroadcastReceiver stepUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getView() != null) {
                updateStepsAndCalories(getView());
            }
        }
    };

    // ── Pick 5 random tips ───────────────────────────────────────────────────

    private void pickRandomTips() {
        // ভাষা অনুযায়ী সঠিক pool বেছে নাও
        String[][] pool = LanguageHelper.isBangla(requireContext()) ? ALL_TIPS_BANGAL : ALL_TIPS;

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < pool.length; i++) indices.add(i);
        Collections.shuffle(indices, new Random());
        for (int i = 0; i < 5; i++) {
            activeTips[i] = pool[indices.get(i)];
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPrefs(requireContext());

        pickRandomTips(); // প্রতিবার fragment create হলে নতুন 5টা random tip

        setupLanguageSwitcher(view);
        setupGreeting(view);
        setupHeaderAvatar(view);
        setupStats(view);
        setupQuickActions(view);
        setupHealthTipSlider(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            setupStats(getView());
            setupGreeting(getView());
            setupHeaderAvatar(getView());
            pickRandomTips();
            startTipAutoSlide();
        }

        IntentFilter filter = new IntentFilter(StepCounterService.BROADCAST_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(stepUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(requireContext(), stepUpdateReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        tipHandler.removeCallbacksAndMessages(null);
        try {
            requireContext().unregisterReceiver(stepUpdateReceiver);
        } catch (IllegalArgumentException ignored) {
        }
    }

    // ── Language Switcher ─────────────────────────────────────────────────────

    private void setupLanguageSwitcher(View v) {
        TextView tvLang = v.findViewById(R.id.tv_lang_switch);
        if (tvLang == null) return;

        boolean isBn = LanguageHelper.isBangla(requireContext());
        // Show what language you'll SWITCH TO (opposite of current)
        tvLang.setText(isBn ? "English" : "বাংলা");

        tvLang.setOnClickListener(x -> {
            String newLang = isBn ? LanguageHelper.LANG_EN : LanguageHelper.LANG_BN;
            LanguageHelper.setLanguage(requireContext(), newLang);
            // Restart the host Activity to apply language change to all views
            if (getActivity() != null) {
                LanguageHelper.restartActivity(getActivity());
            }
        });
    }

    // ── Greeting ─────────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void setupGreeting(View v) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = getString(R.string.greeting_morning);
        else if (hour < 18) greeting = getString(R.string.greeting_afternoon);
        else greeting = getString(R.string.greeting_evening);

        ((TextView) v.findViewById(R.id.tv_greeting)).setText(greeting);
        ((TextView) v.findViewById(R.id.tv_subtitle)).setText(getString(R.string.subtitle_stay_healthy));
    }

    // ── Avatar ───────────────────────────────────────────────────────────────

    private void setupHeaderAvatar(View v) {
        ImageView ivAvatar = v.findViewById(R.id.iv_header_avatar);
        TextView tvInitials = v.findViewById(R.id.tv_header_initials);

        String base64 = prefs.getProfilePicBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ivAvatar.setImageBitmap(bm);
                ivAvatar.setVisibility(View.VISIBLE);
                tvInitials.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {
            }
        }

        String name = prefs.getName();
        if (!name.isEmpty()) {
            String[] parts = name.trim().split(" ");
            String initials = parts.length >= 2 ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0) : String.valueOf(parts[0].charAt(0));
            tvInitials.setText(initials.toUpperCase());
        }
        ivAvatar.setVisibility(View.GONE);
        tvInitials.setVisibility(View.VISIBLE);
    }

    // ── Stats ────────────────────────────────────────────────────────────────

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void setupStats(View v) {
        float bmi = prefs.getLastBmi();
        TextView tvBmi = v.findViewById(R.id.tv_bmi_value);
        TextView tvBmiLabel = v.findViewById(R.id.tv_bmi_label);
        if (bmi > 0) {
            tvBmi.setText(String.format("%.1f", bmi));
            tvBmiLabel.setText(getBmiLabel(bmi));
        } else {
            tvBmi.setText("--");
            tvBmiLabel.setText(getString(R.string.not_set));
        }

        int glasses = prefs.getWaterGlasses();
        float waterL = glasses * 0.25f;
        int waterGoalGlasses = 8;
        int waterPct = Math.min((int) ((glasses / (float) waterGoalGlasses) * 100), 100);
        ((TextView) v.findViewById(R.id.tv_water_value)).setText(String.format("%.1f", waterL));
        ((TextView) v.findViewById(R.id.tv_water_goal)).setText(waterPct + getString(R.string.of_goal));

        tvStepsValue = v.findViewById(R.id.tv_steps_value);
        tvStepsGoal = v.findViewById(R.id.tv_steps_goal);
        tvCalValue = v.findViewById(R.id.tv_cal_value);
        updateStepsAndCalories(v);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateStepsAndCalories(View v) {
        int walkSteps = prefs.getModeSteps("walk");
        int runSteps = prefs.getModeSteps("run");
        int totalSteps = walkSteps + runSteps;

        int stepGoal = prefs.getModeGoal("walk") + prefs.getModeGoal("run");
        if (stepGoal <= 0) stepGoal = 10000;
        int stepPct = Math.min((int) ((totalSteps / (float) stepGoal) * 100), 100);

        if (tvStepsValue == null && v != null) {
            tvStepsValue = v.findViewById(R.id.tv_steps_value);
            tvStepsGoal = v.findViewById(R.id.tv_steps_goal);
            tvCalValue = v.findViewById(R.id.tv_cal_value);
        }

        if (tvStepsValue != null) tvStepsValue.setText(String.format("%,d", totalSteps));
        if (tvStepsGoal != null) tvStepsGoal.setText(stepPct + getString(R.string.of_goal));

        float weight = prefs.getWeight();
        float walkCal = StepCalculatorUtils.getCalories(walkSteps, "walk", weight);
        float runCal = StepCalculatorUtils.getCalories(runSteps, "run", weight);
        int totalCal = Math.round(walkCal + runCal);
        prefs.setCaloriesBurned(totalCal);
        if (tvCalValue != null) tvCalValue.setText(String.valueOf(totalCal));
    }

    private String getBmiLabel(float bmi) {
        if (bmi < 18.5f) return getString(R.string.underweight);
        if (bmi < 25f) return getString(R.string.normal);
        if (bmi < 30f) return getString(R.string.overweight);
        return getString(R.string.obese);
    }

    // ── Quick Actions ────────────────────────────────────────────────────────

    private void setupQuickActions(View v) {
        RecyclerView rv = v.findViewById(R.id.rv_quick_actions);
        rv.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        rv.setNestedScrollingEnabled(true);
        rv.setHasFixedSize(true);

        List<QuickActionItem> actions = new ArrayList<>();
        actions.add(new QuickActionItem(getString(R.string.qa_medicine_reminder), R.drawable.ic_pill, R.drawable.circle_stat_medicine, MedicineReminderActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_step_counter), R.drawable.ic_step, R.drawable.circle_stat_steps, StepCounterActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_water_alarm), R.drawable.ic_drop, R.drawable.circle_stat_water, WaterIntakeAlarmActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_water_intake), R.drawable.ic_water, R.drawable.circle_stat_water, WaterIntakeActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_calorie_calculator), R.drawable.ic_calorie_cal, R.drawable.circle_stat_calorie_calculator, CalorieCalculatorActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_calculate_bmi), R.drawable.ic_bmi, R.drawable.circle_stat_bmi, BmiCalculatorActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_blood_pressure), R.drawable.ic_blood_pressure, R.drawable.circle_stat_bp, BloodPressureActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_blood_sugar), R.drawable.ic_blood_sugar, R.drawable.circle_stat_blood_sugar, BloodSugarActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_ideal_weight), R.drawable.ic_weight, R.drawable.circle_stat_ideal_weight, IdealWeightActivity.class));
        actions.add(new QuickActionItem(getString(R.string.qa_calories_burned), R.drawable.ic_calorie, R.drawable.circle_stat_calories_burned, CaloriesBurnedActivity.class));

        QuickActionAdapter adapter = new QuickActionAdapter(actions, (item, position) -> {
            String title = item.getTitle();
            String medRemTitle = getString(R.string.qa_medicine_reminder);
            String stepTitle = getString(R.string.qa_step_counter);
            String waterAlarmTitle = getString(R.string.qa_water_alarm);
            if (medRemTitle.equals(title)) {
                navigateInMain(R.id.nav_reminders);
            } else if (stepTitle.equals(title)) {
                navigateInMain(R.id.nav_activity);
            } else if (waterAlarmTitle.equals(title)) {
                navigateInMain(R.id.nav_water);
            } else if (item.getTargetActivity() != null) {
                startActivity(new Intent(getContext(), item.getTargetActivity()));
            }
        });
        rv.setAdapter(adapter);

        v.findViewById(R.id.card_bmi).setOnClickListener(x -> startActivity(new Intent(getContext(), BmiCalculatorActivity.class)));
        v.findViewById(R.id.card_water).setOnClickListener(x -> startActivity(new Intent(getContext(), WaterIntakeActivity.class)));
        v.findViewById(R.id.card_steps).setOnClickListener(x -> navigateInMain(R.id.nav_activity));
        v.findViewById(R.id.card_calories_burned).setOnClickListener(x -> startActivity(new Intent(getContext(), CaloriesBurnedActivity.class)));
        v.findViewById(R.id.tv_header_initials).setOnClickListener(x -> navigateInMain(R.id.nav_profile));
    }

    private void navigateInMain(int navItemId) {
        if (getActivity() instanceof MainActivity) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(navItemId);
            }
        }
    }

    // ── Health Tip Auto-Slider ───────────────────────────────────────────────

    private static final long RESUME_DELAY_MS = 3000L;
    private boolean isUserHolding = false;

    private void setupHealthTipSlider(View v) {
        cardHealthTip = v.findViewById(R.id.card_health_tip);
        tvTipIcon = v.findViewById(R.id.tv_tip_icon);
        tvTipTitle = v.findViewById(R.id.tv_tip_title);
        tvTipText = v.findViewById(R.id.tv_health_tip);
        tipDotContainer = v.findViewById(R.id.tip_dot_container);

        currentTipIndex = 0;
        buildDots();
        showTip(currentTipIndex, false);
        startTipAutoSlide();
        setupTipGesture();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTipGesture() {
        if (cardHealthTip == null) return;

        final float[] downX = {0f};
        final float SWIPE_THRESHOLD = dpToPx(40);

        cardHealthTip.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX[0] = event.getX();
                    isUserHolding = true;
                    tipHandler.removeCallbacks(tipRunnable);
                    tipHandler.removeCallbacks(resumeAutoRunnable);
                    break;

                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    isUserHolding = false;
                    float deltaX = event.getX() - downX[0];

                    if (Math.abs(deltaX) >= SWIPE_THRESHOLD) {
                        if (deltaX < 0) {
                            currentTipIndex = (currentTipIndex + 1) % activeTips.length;
                        } else {
                            currentTipIndex = (currentTipIndex - 1 + activeTips.length) % activeTips.length;
                        }
                        showTip(currentTipIndex, true);
                    }

                    tipHandler.removeCallbacks(resumeAutoRunnable);
                    tipHandler.postDelayed(resumeAutoRunnable, RESUME_DELAY_MS);
                    break;
            }
            return true;
        });
    }

    private final Runnable resumeAutoRunnable = () -> {
        if (!isUserHolding) {
            startTipAutoSlide();
        }
    };

    private void buildDots() {
        if (tipDotContainer == null) return;
        tipDotContainer.removeAllViews();
        for (int i = 0; i < activeTips.length; i++) {
            View dot = new View(requireContext());
            int size = dpToPx(6);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == currentTipIndex ? R.drawable.tip_dot_active : R.drawable.tip_dot_inactive);
            tipDotContainer.addView(dot);
        }
    }

    private void updateDots(int activeIndex) {
        if (tipDotContainer == null) return;
        for (int i = 0; i < tipDotContainer.getChildCount(); i++) {
            tipDotContainer.getChildAt(i).setBackgroundResource(i == activeIndex ? R.drawable.tip_dot_active : R.drawable.tip_dot_inactive);
        }
    }

    private void showTip(int index, boolean animate) {
        String[] tip = activeTips[index];
        String icon = tip[0];
        String text = tip[1];
        int color = android.graphics.Color.parseColor(tip[2]);

        if (animate && cardHealthTip != null) {
            cardHealthTip.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                applyTip(icon, text, color);
                cardHealthTip.animate().alpha(1f).setDuration(250).start();
            }).start();
        } else {
            applyTip(icon, text, color);
        }
        updateDots(index);
    }

    private void applyTip(String icon, String text, int color) {
        if (cardHealthTip != null) cardHealthTip.setCardBackgroundColor(color);
        if (tvTipIcon != null) tvTipIcon.setText(icon);
        if (tvTipText != null) tvTipText.setText(text);
    }

    private final Runnable tipRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isUserHolding) {
                currentTipIndex = (currentTipIndex + 1) % activeTips.length;
                showTip(currentTipIndex, true);
                tipHandler.postDelayed(this, TIP_INTERVAL_MS);
            }
        }
    };

    private void startTipAutoSlide() {
        tipHandler.removeCallbacks(tipRunnable);
        tipHandler.removeCallbacks(resumeAutoRunnable);
        tipHandler.postDelayed(tipRunnable, TIP_INTERVAL_MS);
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}