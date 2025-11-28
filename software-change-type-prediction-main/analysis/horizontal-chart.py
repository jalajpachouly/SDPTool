# Import necessary libraries
import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

# Ensure that plots are displayed inline if using Jupyter Notebook
# Uncomment the following line if using Jupyter Notebook
# %matplotlib inline

# Define the data
data = {
    'Change Type': [
        'changetype_core',
        'changetype_file',
        'changetype_tcp_udp',
        'changetype_build',
        'changetype_jdbc',
        'changetype_jms',
        'changetype_redis',
        'changetype_ftp',
        'changetype_mail'
    ],
    'Instances': [
        994,
        180,
        154,
        144,
        122,
        120,
        109,
        93,
        87
    ]
}

# Create a DataFrame
df = pd.DataFrame(data)

# Sort the DataFrame for better visualization (optional)
df = df.sort_values('Instances', ascending=True)

# Set the style for the plot
sns.set(style="whitegrid")

# Initialize the matplotlib figure
plt.figure(figsize=(10, 6))

# Create a horizontal barplot
barplot = sns.barplot(
    x='Instances',
    y='Change Type',
    data=df,
    palette='viridis'  # You can choose other palettes like 'magma', 'plasma', etc.
)

# Add titles and labels
plt.title('Instance Counts per Changetype Category', fontsize=16, fontweight='bold')
plt.xlabel('Number of Instances', fontsize=14)
plt.ylabel('Changetype Category', fontsize=14)

# Annotate each bar with the count
for index, value in enumerate(df['Instances']):
    plt.text(
        value + 5,  # Position the text slightly to the right of the bar
        index,  # y-position corresponds to the bar's position
        str(value),
        va='center',
        fontsize=12
    )

# Adjust layout for better fit
plt.tight_layout()

# Save the plot as an image file
plt.savefig('changetype_horizontal_bar_chart.png', dpi=300)

# Display the plot
plt.show()
